import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class KeyedHash {
    private static int block_size = 64; // 64 bytes = 512 bits
    private static String hashInKeyedHash = "SHA-512";

    public static byte[] encrypt(byte[] msg, byte[] key) {
        int padding_size = ((block_size - msg.length) % block_size + block_size) % block_size;
        byte[] paddedMsg = new byte[msg.length + padding_size];
        for (int i = 0; i < msg.length; i++) {
            paddedMsg[i] = msg[i];
        }

        byte[] encrypt_msg = new byte[paddedMsg.length];
        try {
            // byte[] IV = new byte[16];
            // new SecureRandom().nextBytes(IV);
            MessageDigest digest = MessageDigest.getInstance(hashInKeyedHash);
            byte[] b_i = digest.digest(key);
            int i = 0;
            while (i < paddedMsg.length) {
                byte[] p_i = Arrays.copyOfRange(paddedMsg, i, i + block_size);
                byte[] c_i = new byte[block_size];
                for (int j = 0; j < block_size; j++) {
                    c_i[j] = (byte) (p_i[j] ^ b_i[j]);
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(key);
                outputStream.write(c_i);
                b_i = digest.digest(outputStream.toByteArray());

                for (int j = 0; j < block_size; j++) {
                    encrypt_msg[i + j] = c_i[j];
                }

                i += block_size;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return encrypt_msg;
    }

    public static byte[] decrypt(byte[] encrypt_msg, byte[] key) {
        byte[] msg = new byte[encrypt_msg.length];
        try {
            MessageDigest digest = MessageDigest.getInstance(hashInKeyedHash);
            byte[] b_i = digest.digest(key);
            int i = 0;
            while (i < msg.length) {
                byte[] c_i = Arrays.copyOfRange(encrypt_msg, i, i + block_size);
                for (int j = 0; j < block_size; j++) {
                    msg[i + j] = (byte) (c_i[j] ^ b_i[j]);
                }
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(key);
                outputStream.write(c_i);
                b_i = digest.digest(outputStream.toByteArray());
                i += block_size;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return msg;
    }

    public static void main(String[] args) {
        String test = "012345678911234567892134567893123456789412345678951234567896123aaaaa";
        System.out.println(test.getBytes(StandardCharsets.UTF_8).length);
        SecureRandom random = new SecureRandom();
        byte key[] = new byte[64];
        random.nextBytes(key);

        byte[] res = decrypt(encrypt(test.getBytes(StandardCharsets.UTF_8), key), key);
        res = Helper.trimNullBytes(res);
        String s = new String(res, StandardCharsets.UTF_8);

        System.out.println(s);
    }
}