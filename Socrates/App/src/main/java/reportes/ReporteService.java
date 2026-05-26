package reportes;

import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import database.Conexion;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

public class ReporteService {

    public static File generarComprobanteVenta(long idVenta, String plantilla) throws Exception {
        // Preferir .jasper precompilado en target/classes/reportes
        String baseName = plantilla.replaceFirst("\\\\.jrxml$", "");
        File precompiled = new File(new File("").getAbsolutePath(), "target/classes/reportes/" + baseName + ".jasper");

        Map<String, Object> params = new HashMap<>();
        params.put("idventa", idVenta);

        try (Connection con = Conexion.getInstancia().conectar()) {
            JasperPrint jp;
            if (precompiled.exists()) {
                System.out.println("Usando .jasper precompilado: " + precompiled.getAbsolutePath());
                jp = JasperFillManager.fillReport(precompiled.getAbsolutePath(), params, con);
            } else {
                System.out.println("No se encontró .jasper precompilado, compilando JRXML: " + plantilla);
                try (InputStream is = findReportStream(plantilla)) {
                    JasperReport jr;
                    if (is != null) {
                        jr = JasperCompileManager.compileReport(is);
                        System.out.println("Compilado desde classpath: " + plantilla);
                    } else {
                        File f = new File(new File("").getAbsolutePath(), "src/reportes/" + plantilla);
                        if (f.exists()) {
                            jr = JasperCompileManager.compileReport(f.getAbsolutePath());
                            System.out.println("Compilado desde disco: " + f.getAbsolutePath());
                        } else throw new IllegalStateException("Plantilla no encontrada: " + plantilla);
                    }
                    jp = JasperFillManager.fillReport(jr, params, con);
                }
            }

            int pages = (jp.getPages() != null) ? jp.getPages().size() : 0;
            System.out.println("Páginas generadas en JasperPrint: " + pages);
            if (pages == 0) throw new IllegalStateException("El reporte no contiene páginas (posible falta de datos para idVenta=" + idVenta + ")");

            Path outDir = Path.of(new File("").getAbsolutePath(), "target", "reportes");
            Files.createDirectories(outDir);
            File outPdf = outDir.resolve("comprobante_" + idVenta + ".pdf").toFile();
            System.out.println("Exportando PDF a: " + outPdf.getAbsolutePath());
            JasperExportManager.exportReportToPdfFile(jp, outPdf.getAbsolutePath());

            if (Desktop.isDesktopSupported()) {
                try { Desktop.getDesktop().open(outPdf); } catch (Exception ignore) { System.out.println("No se pudo abrir el PDF automáticamente: " + ignore.getMessage()); }
            }
            return outPdf;
        }
    }

    private static InputStream findReportStream(String name) {
        InputStream is = ReporteService.class.getResourceAsStream("/" + name);
        if (is != null) return is;
        return ReporteService.class.getResourceAsStream("/reportes/" + name);
    }
}
