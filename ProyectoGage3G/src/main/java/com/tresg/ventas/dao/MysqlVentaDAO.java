package com.tresg.ventas.dao;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import com.tresg.almacen.jpa.DetalleAlmacenJPA;
import com.tresg.almacen.jpa.DetalleAlmacenJPAPK;
import com.tresg.incluido.jpa.EstadoJPA;
import com.tresg.incluido.jpa.ProductoJPA;
import com.tresg.util.formato.Formateo;
import com.tresg.util.jpa.JpaUtil;
import com.tresg.util.optional.ValoresNulos;
import com.tresg.util.stock.ActualizarExistencia;
import com.tresg.ventas.interfaz.VentaDAO;
import com.tresg.ventas.jpa.CobranzaFacturaJPAPK;
import com.tresg.ventas.jpa.CobranzaJPA;
import com.tresg.ventas.jpa.DetalleVentaJPA;
import com.tresg.ventas.jpa.DetalleVentaJPAPK;
import com.tresg.ventas.jpa.VentaJPA;

public class MysqlVentaDAO implements VentaDAO {

	EntityManager em = null;
	private void open() {
		 em = JpaUtil.getEntityManager();
	}
	
	private void close() {
		 em.close();
	}

	ActualizarExistencia stockUtil;
	//para obtener cadena hora
	Formateo formato;

	@SuppressWarnings("unchecked")
	@Override
	public List<VentaJPA> listarVenta() {
		open();
		Query q = em.createNamedQuery(VentaJPA.LISTAR_VENTAS);

		return q.getResultList();
	}
	
	@Override
	public VentaJPA mostrarDetalleVenta(int comprobante) {
		open();
		VentaJPA objVenta = em.find(VentaJPA.class, comprobante);
		em.refresh(objVenta);
		
		return objVenta;
	}

	@Override
	public int comprobarExistenciaProducto(int producto) {
		open();

		Query q = em.createNamedQuery(DetalleAlmacenJPA.VERIFICAR_EXISTENCIA).setParameter("p1", producto);
		try {
			return Integer.valueOf( q.getSingleResult().toString());
		} catch (NoResultException e) {
			return 0;
		}	
	}

	@Override
	public String registrarVenta(VentaJPA venta) {
		open();

		em.getTransaction().begin();
		try {
			em.persist(venta);
		} catch (RuntimeException e) {
			em.getTransaction().rollback();
			throw e;
		}

		CobranzaJPA objCobranza;
		/* venta a credito entonces se genera una cobranza */
		if (venta.getPago().getCodPago() == 2) {
			objCobranza = registroVendeCobra(venta.getNumComprobante(), venta.getMonto(), venta.getFecha());
			try {
				em.persist(objCobranza);
			} catch (RuntimeException e) {
				em.getTransaction().rollback();
				throw e;
			}
		}
		
		stockUtil=new ActualizarExistencia();
		DetalleAlmacenJPAPK dapk=new DetalleAlmacenJPAPK();
		DetalleAlmacenJPA da=new DetalleAlmacenJPA();
		
		for (DetalleVentaJPA d : venta.getDetalles()) {
			dapk.setCodAlmacen(3);
			dapk.setCodProducto(d.getId().getCodProducto());
			da.setId(dapk);
			stockUtil.actualizarAlmacenDecremento(d.getCantidad(), da);

		}
		
		em.getTransaction().commit();
		close();

		return "Venta registrada exitosamente";
	}

	@Override
	public String actualizarVenta(VentaJPA venta) {
		open();

		em.getTransaction().begin();

		DetalleVentaJPA objDetalle;

		for (DetalleVentaJPA p : venta.getDetalles()) {

			objDetalle = em.find(DetalleVentaJPA.class, p.getId());

		}

		try {
			em.merge(venta);
		} catch (RuntimeException e) {
			em.getTransaction().rollback();
			throw e;
		}

		/* venta a credito entonces se genera una cobranza */
		if (venta.getPago().getCodPago() == 2) {
			CobranzaJPA objCobranza = registroVendeCobra(venta.getNumComprobante(), venta.getMonto(), venta.getFecha());
			try {
				em.merge(objCobranza);
			} catch (RuntimeException e) {
				em.getTransaction().rollback();
				throw e;
			}

		}

		em.getTransaction().commit();
		close();

		return "Venta actualizada exitosamente";
	}

	@Override
	public void actualizarItemVentaEliminada(ProductoJPA producto) {
		open();

		em.getTransaction().begin();

		DetalleAlmacenJPA da = new DetalleAlmacenJPA();
		DetalleAlmacenJPAPK dapk = new DetalleAlmacenJPAPK();
		dapk.setCodAlmacen(3);
		dapk.setCodProducto(producto.getCodProducto());
		da.setId(dapk);
		Query q3 = em.createNamedQuery(DetalleAlmacenJPA.ACTUALIZAR_ALMACEN_INCREMENTO);
		q3.setParameter("p1", producto.getCantidad());
		q3.setParameter("p2", da.getId());
		q3.executeUpdate();
		em.getTransaction().commit();
		close();

	}

	@Override
	public String anularVenta(int venta) {
		open();
		
		em.getTransaction().begin();
		EstadoJPA objEstado=new EstadoJPA();
		objEstado.setCodEstado(3);
		try {			
			Query q = em.createNamedQuery(VentaJPA.ACTUALIZAR_VENTA_ESTADO);
			q.setParameter("x", objEstado);
			q.setParameter("y", venta);
			q.executeUpdate();

		} catch (RuntimeException e) {
			em.getTransaction().rollback();
			throw e;
		}

		// actualizar el almacen
		VentaJPA objVenta = em.find(VentaJPA.class, venta);
		em.refresh(objVenta);

		em.getTransaction().commit();
		close();
		return "Venta anulada";
	}

	CobranzaJPA registroVendeCobra(int comprobante, BigDecimal monto, Date fecha) {
		
		formato=new Formateo();
		CobranzaFacturaJPAPK cf = new CobranzaFacturaJPAPK();
		cf.setNumComprobante(comprobante);
		cf.setNumCobranza(0);

		CobranzaJPA objCobranza = new CobranzaJPA();
		objCobranza.setId(cf);
		objCobranza.setFecha(fecha);
		objCobranza.setHora(formato.obtenerHora());
		objCobranza.setMontoSaldo(monto);
		objCobranza.setMontoPago(new java.math.BigDecimal("0.00"));

		return objCobranza;

	}


}