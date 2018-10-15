package com.tresg.util.correo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Mensajeria {


	public void envioFirma(String cliente, String factura, String fecha, String monto, String firma, String destinatario) throws Exception {
		// Propiedades de la conexi�n
		Properties props = new Properties();

		props.setProperty("mail.smtp.host", "smtp-mail.outlook.com");
		props.setProperty("mail.smtp.starttls.enable", "true");
		props.setProperty("mail.smtp.port", "587");
		props.setProperty("mail.smtp.user", "muygel@hotmail.com");
		props.setProperty("mail.smtp.auth", "true");

		// Preparamos la sesion
		Session sesionEmail = Session.getDefaultInstance(props, null);
		sesionEmail.setDebug(true);

		/** MultiPart para crear mensajes compuestos. */
		MimeMultipart multiParte = new MimeMultipart();

		// Construir un correo de texto con un adjunto
		BodyPart texto = new MimeBodyPart();
		Map<String, String> input = new HashMap<String, String>();
		input.put("cliente", cliente);
		input.put("factura", factura);
		input.put("fecha", fecha);
		input.put("monto", monto);
		String contenidoHTML = leerEmailDesdeHtml("C:/repositorioGitGage3G/ProyectoGage3G/src/main/webapp/email.xhtml", input);
		texto.setContent(contenidoHTML, "text/html");
		multiParte.addBodyPart(texto);

		BodyPart adjunto = new MimeBodyPart();
		adjunto.setDataHandler(
				new DataHandler(new FileDataSource("C:\\SFS_v1.2\\sunat_archivos\\sfs\\FIRMA\\" + firma + ".xml")));
		adjunto.setFileName(firma + ".xml");
		multiParte.addBodyPart(adjunto);

		// Lo enviamos.
		Transport t = sesionEmail.getTransport();// "smtp"

		// Construimos el mensaje
		MimeMessage message = new MimeMessage(sesionEmail);
		// Agregar quien env�a el correo
		message.setFrom(new InternetAddress("muygel@hotmail.com"));
		// Se rellenan los destinatarios
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));
		// Se rellena el subject
		message.setSubject("EXTINTORES CAPELO PERU te ha enviado la Factura Electronica : "+ factura);
		// Se mete el texto y la foto adjunta.
		message.setContent(multiParte, "text/html");

		t.connect("muygel@hotmail.com", "gage24neyra");
		t.sendMessage(message, message.getAllRecipients());
		t.close();

	}

	protected String leerEmailDesdeHtml(String ruta, Map<String, String> entrada) {
		
		String msg = leerContenidoDesdeArchivo(ruta);
		try {
			Set<Entry<String, String>> entries = entrada.entrySet();
			for (Map.Entry<String, String> entry : entries) {
				msg = msg.replace(entry.getKey().trim(), entry.getValue().trim());
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return msg;
	}

	private String leerContenidoDesdeArchivo(String archivo) {
		
		StringBuffer contenidos = new StringBuffer();

		try {
			// use buffering, reading one line at a time
			BufferedReader reader = new BufferedReader(new FileReader(archivo));
			try {
				String line = null;
				while ((line = reader.readLine()) != null) {
					contenidos.append(line);
					contenidos.append(System.getProperty("line.separator"));
				}
			} finally {
				reader.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return contenidos.toString();
	}

}
