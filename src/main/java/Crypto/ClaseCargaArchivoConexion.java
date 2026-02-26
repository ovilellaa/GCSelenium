package Crypto;

import java.io.*;
import java.util.Arrays;

public class ClaseCargaArchivoConexion {

    private static byte[] byteClave;
    private static byte[] byteVal;

    private static CryptoUtils objectEncripG = new CryptoUtils(CryptoUtils.Tipo.TripleDes);
    private static String stringArchivG = "";

    // ----------------- API pública -----------------

    public static boolean MTDCargaDatosArchivo(String stringNfile) {
        boolean valResul = true;

        // Ruta externa (junto al JAR)
        String externalPath = "configuration/" + stringNfile;
        File externalFile = new File(externalPath);

        if (!externalFile.exists()) {
            return false;

        }

        try (InputStream is = new BufferedInputStream(new FileInputStream(externalFile))) {

            int int32Largo = readInt32LE(is);                 // longitud clave (LE)
            byteClave = readBytes(is, int32Largo);            // bytes clave

            int32Largo = readInt32LE(is);                     // longitud IV (LE)
            byteVal = readBytes(is, int32Largo);              // bytes IV

            // Leer filas hasta EOF
            while (MTDCargaInformacion(is)) {
                stringArchivG += "\r\n" + MTDQuitarEncriptacion(lastReadString);
            }

        } catch (Exception e) {
            valResul = false;
        }

        return valResul;
    }

    public static String getStringArchivG() {
        return stringArchivG;
    }

    // ----------------- Estado interno -----------------

    private static String lastReadString = "";

    // ----------------- Lectura de filas (formato .NET) -----------------

    private static boolean MTDCargaInformacion(InputStream is) {
        try {
            lastReadString = readCustomString(is);  // aquí llamas al nuevo método
        } catch (IOException eof) {
            return false; // fin de archivo
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }


    private static String readStringCharsLE(InputStream is, int charCount) throws IOException {
        byte[] buf = readBytes(is, charCount * 2); // cada char = 2 bytes
        return new String(buf, "UTF-16LE");
    }


    // ----------------- Desencriptado -----------------

    private static String MTDQuitarEncriptacion(String stringResult) {
        try {
            return objectEncripG.desEncriptarTDes(stringResult, byteClave, byteVal);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // ----------------- Helpers de lectura Little-Endian -----------------

    private static String readCustomString(InputStream is) throws IOException {
        int length = readInt32LE(is); // longitud en "caracteres" según el escritor

        // Probe: inspecciona los dos próximos bytes
        if (!is.markSupported()) {
            // Envuelve si no soporta mark/reset
            is = new BufferedInputStream(is);
        }
        BufferedInputStream bis = (is instanceof BufferedInputStream)
                ? (BufferedInputStream) is
                : new BufferedInputStream(is);

        bis.mark(4);
        int b0 = bis.read(); // debería ser 0x0B si coincide con .NET
        int b1 = bis.read(); // si UTF-16LE, debería ser 0x00
        bis.reset();

        // Descarta el/los char(es) según la lógica original
        if (length >= 128) {
            discardChar(bis, b0, b1); // descarta 1 char según encoding detectado
            // Re‑probe para el siguiente descarte
            bis.mark(2);
            b0 = bis.read();
            b1 = bis.read();
            bis.reset();
        }
        discardChar(bis, b0, b1);

        // Ahora lee la cadena según encoding detectado
        if (isUtf16Pair(b0, b1)) {
            // UTF-16LE: cada "carácter" son 2 bytes
            byte[] raw = readBytes(bis, length * 2);
            return new String(raw, "UTF-16LE");
        } else {
            // Un‑byte por carácter (ASCII/UTF-8). OJO: aquí 'length' es bytes, no code units.
            byte[] raw = readBytes(bis, length);
            return new String(raw, "UTF-8"); // o "US-ASCII" si seguro que es ASCII
        }
    }

    private static boolean isUtf16Pair(int lo, int hi) {
        // Heurística: si el segundo byte es 0x00, asumimos UTF-16LE (pares X,0)
        return hi == 0x00;
    }

    private static void discardChar(InputStream is, int peekLo, int peekHi) throws IOException {
        if (isUtf16Pair(peekLo, peekHi)) {
            // Descarta 2 bytes (un code unit UTF-16LE)
            readBytes(is, 2);
        } else {
            // Descarta 1 byte (carácter un‑byte)
            readBytes(is, 1);
        }
    }

    // Helpers básicos
    private static int readInt32LE(InputStream is) throws IOException {
        int b0 = is.read(), b1 = is.read(), b2 = is.read(), b3 = is.read();
        if ((b0 | b1 | b2 | b3) < 0) throw new IOException("EOF Int32");
        return (b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 24);
    }

    private static byte[] readBytes(InputStream is, int len) throws IOException {
        byte[] buf = new byte[len];
        int off = 0;
        while (off < len) {
            int r = is.read(buf, off, len - off);
            if (r < 0) throw new IOException("EOF leyendo " + len + " bytes");
            off += r;
        }
        return buf;
    }


    private static char readCharLE(InputStream is) throws IOException {
        int lo = is.read(); // byte bajo
        int hi = is.read(); // byte alto
        if ((lo | hi) < 0) throw new IOException("EOF al leer Char");
        int codeUnit = (lo & 0xFF) | ((hi & 0xFF) << 8);
        return (char) codeUnit; // UTF-16 LE code unit
    }


}
