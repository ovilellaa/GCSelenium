package Crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;


public class CryptoUtils {

    public enum Tipo {
        MD5,
        SHA1,
        DES,
        TripleDes
    }

    private Tipo tipo;

    public CryptoUtils(Tipo tipo) {
        this.tipo = tipo;
    }

    private Cipher getCipher(int mode, byte[] key, byte[] iv) throws Exception {
        String algorithm = "";
        switch (tipo) {
            case DES:
                algorithm = "DES/CBC/PKCS5Padding";
                break;
            case TripleDes:
                algorithm = "DESede/CBC/PKCS5Padding";
                break;
            default:
                throw new IllegalArgumentException("Tipo no soportado para cifrado simétrico");
        }

        SecretKey secretKey = new SecretKeySpec(key, algorithm.startsWith("DESede") ? "DESede" : "DES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(mode, secretKey, ivSpec);
        return cipher;
    }

    public String desEncriptarTDes(String valueToDes, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, key, iv);

        byte[] decoded = base64UrlDecode(valueToDes);
        byte[] decrypted = cipher.doFinal(decoded);

        return new String(decrypted, "UTF-8");
    }

    private byte[] base64UrlDecode(String arg) {
        String s = arg.replace("-", "+").replace("_", "/");
        switch (s.length() % 4) {
            case 0:
                break;
            case 2:
                s += "==";
                break;
            case 3:
                s += "=";
                break;
            default:
                throw new IllegalArgumentException("Illegal base64url string!");
        }
        return Base64.getDecoder().decode(s);
    }

}
