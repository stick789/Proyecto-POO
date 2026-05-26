package reportes;

import java.io.File;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;

public class PrecompileReports {

    public static void main(String[] args) {
        File srcDir = new File("src/reportes");
        File outDir = new File("target/classes/reportes");
        if (!outDir.exists()) outDir.mkdirs();

        if (!srcDir.exists()) {
            System.err.println("No existe la carpeta src/reportes — nada que compilar.");
            return;
        }

        File[] jrxmls = srcDir.listFiles((d, name) -> name.toLowerCase().endsWith(".jrxml"));
        if (jrxmls == null || jrxmls.length == 0) {
            System.out.println("No se encontraron archivos .jrxml en src/reportes");
            return;
        }

        for (File jrxml : jrxmls) {
            File out = new File(outDir, jrxml.getName().replaceFirst("\\\\.jrxml$", ".jasper"));
            System.out.println("Compilando: " + jrxml.getAbsolutePath() + " -> " + out.getAbsolutePath());
            try {
                JasperCompileManager.compileReportToFile(jrxml.getAbsolutePath(), out.getAbsolutePath());
            } catch (JRException e) {
                System.err.println("Error compilando " + jrxml.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
