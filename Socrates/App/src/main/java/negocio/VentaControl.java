package negocio;

import java.io.File;

import reportes.ReporteService;

public class VentaControl {

    /**
     * Genera y muestra el comprobante PDF para la venta indicada.
     * Lanza excepciones en caso de fallo.
     */
    public static File reporteComprobante(long idVenta) throws Exception {
        // Plantilla por defecto — ajusta el nombre según tu JRXML
        String plantilla = "RPTUsuarios.jrxml";
        return ReporteService.generarComprobanteVenta(idVenta, plantilla);
    }
}
