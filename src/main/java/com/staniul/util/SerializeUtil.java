package com.staniul.util;

import java.io.*;

/**
 * Utility class for serialization. Reading and writing objects.
 */
public class SerializeUtil {
    /**
     * Serializes object to a file with a given name. Given object needs to implement {@code Serializable} interface.
     *
     * @param fileName Name of file to which object should be serialized.
     * @param object   Object to be serialized, must implement Serializable interface.
     *
     * @throws IOException When folder does not exists or there is another problem with writing to file, maybe
     *                     permissions?
     */
    public static void serialize(String fileName, Object object) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName, false))) {
            oos.writeObject(object);
        }
    }

    /**
     * Reads object form a file.
     *
     * @param fileName Name of file to which object was serialized.
     * @param <T>      Type of class to read.
     *
     * @return Read object of specified type from file.
     *
     * @throws IOException            When there is problem with reading from file, file does not exists.
     * @throws ClassNotFoundException When read object is not of specified type.
     * @throws ClassCastException     When specified type does not match.
     */
    public static <T> T deserialize(String fileName) throws IOException, ClassNotFoundException, ClassCastException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            return (T) ois.readObject();
        }
    }
}
