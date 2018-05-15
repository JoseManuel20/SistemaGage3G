package com.tresg.incluido.interfaz;

import java.util.List;

import com.tresg.incluido.jpa.ProductoJPA;

public interface ProductoDAO {

	// CU Mantenimiento productos
	List<ProductoJPA> buscarProductoPorDescripcion(String descripcion);

	ProductoJPA buscarProductoPorCodigo(int codigo);

	String actualizarProducto(ProductoJPA producto);

	String eliminarProducto(int codigo);

}
