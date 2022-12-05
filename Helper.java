import javax.sound.midi.SysexMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Helper {
    private static String hashFunction = "SHA-256";

    public static byte[] combineBytes(byte[] first, byte[] second) {
        byte[] combined = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        return combined;
    }

    public static void sendEncrypt(ObjectOutputStream objOut, String message, byte[] sessionKey) throws Exception {
        // encrypt the message
        byte[] plaintext = message.getBytes(StandardCharsets.UTF_8);
        sendEncrypt(objOut, plaintext, sessionKey);
    }

    public static void sendEncrypt(ObjectOutputStream objOut, byte[] plaintext, byte[] sessionKey) throws Exception {
        // encrypt the message
        byte[] ciphertext = KeyedHash.encrypt(plaintext, sessionKey);

        // create hash for integrity verification
        MessageDigest digest = MessageDigest.getInstance(hashFunction);
        byte[] messageKey = combineBytes(plaintext, sessionKey);
        byte[] hash = digest.digest(messageKey);

        // send message
        objOut.writeObject(ciphertext);
        objOut.writeObject(hash);
        objOut.flush();
    }

    public static String recvDecrypt(ObjectInputStream objIn, byte[] sessionKey) throws Exception {
        // receive and decrypt the message
        byte[] plaintext = recvDecryptBytes(objIn, sessionKey);
        return new String(plaintext);
    }

    public static byte[] recvDecryptBytes(ObjectInputStream objIn, byte[] sessionKey) throws Exception {
        // receive and decrypt the message
        byte[] ciphertext = (byte[]) objIn.readObject();
        byte[] plaintext = KeyedHash.decrypt(ciphertext, sessionKey);
        plaintext = Helper.trimNullBytes(plaintext);

        // receive the hash and verify integrity
        byte[] hashReceived = (byte[]) objIn.readObject();
        MessageDigest digest = MessageDigest.getInstance(hashFunction);
        byte[] messageKey = combineBytes(plaintext, sessionKey);
        byte[] hashCalculated = digest.digest(messageKey);

        if (Arrays.equals(hashCalculated, hashReceived)) {
            //System.out.println("Hash matched\n");
        } else {
            //System.out.println("Hash not matching\n");
            throw new Exception("Integrity hash not matching");
        }

        return plaintext;
    }

    public static byte[] trimNullBytes(byte[] bytes) {
        if (bytes.length == 0) return bytes;
        int i = bytes.length - 1;
        while (bytes[i] == 0) {
            i--;
        }
        byte[] copy = Arrays.copyOfRange(bytes, 0, i + 1);
        return copy;
    }

    public static String bytesToHexString(byte[] bytes) {
        String hexString = "0x";
        for (byte b : bytes) {
            hexString += String.format("%02x", b);
        }
        return hexString;
    }

    public static long bytesToLong(byte[] longBytes){
        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);
        byteBuffer.put(longBytes);
        byteBuffer.flip();
        return byteBuffer.getLong();
    }

    public static byte[] longToBytes(long data) {
        return new byte[]{
                (byte) ((data >> 56) & 0xff),
                (byte) ((data >> 48) & 0xff),
                (byte) ((data >> 40) & 0xff),
                (byte) ((data >> 32) & 0xff),
                (byte) ((data >> 24) & 0xff),
                (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 8) & 0xff),
                (byte) ((data >> 0) & 0xff),
        };
    }

    public static void main(String[] args) {
        long num = 123456789101112L;
        System.out.println(bytesToLong(longToBytes(num)));
    }

}
