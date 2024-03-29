// Clase de cliente que define la interfaz a las aplicaciones.
// Proporciona la misma API que RandomAccessFile.
package afs;

import java.rmi.*;
import java.io.*;

public class VenusFile {
    public static final String cacheDir = "Cache/";
    public RandomAccessFile file;
    public Venus venus;
    public String fileName;
    public String mode;
    public boolean written;
    public boolean size;


    public VenusFile(Venus venus, String fileName, String mode) throws RemoteException, IOException, FileNotFoundException {
        this.mode = mode;
        this.fileName = fileName;
        this.venus = venus;
        if (mode.equals("rw")) {
            if (existeEnCache(fileName)) {
                file = new RandomAccessFile(cacheDir + fileName, mode);
            } else {
                file = copiarDeCache(venus, fileName, mode);
            }
        } else if (mode.equals("r")) {
            try {
                file = new RandomAccessFile(cacheDir + fileName, mode);    
            } catch (FileNotFoundException e) {
                file = copiarDeCache(venus, fileName, mode);
            }
            
        }

    }

    public int read(byte[] b) throws RemoteException, IOException {
        return file.read(b);
    }

    public void write(byte[] b) throws RemoteException, IOException {
        written = true;
        file.write(b);
    }

    public void seek(long p) throws RemoteException, IOException {
        file.seek(p);
    }

    public void setLength(long l) throws RemoteException, IOException {
        size = true;
        file.setLength(l);
    }

    public void close() throws RemoteException, IOException {
        if (this.mode.equals("rw")) {
            if (this.written) {
                ViceWriter viceWriter = venus.getsrvVice().upload(this.fileName, this.mode);
                int blockSize = Integer.parseInt(venus.getTam());
                byte[] buf = new byte[blockSize];
                file.seek(0);
                int bytesRead = 0;
                while((bytesRead = file.read(buf)) != -1) {
                    if (bytesRead < blockSize) {
                        byte [] dest2 = new byte[bytesRead];
                        for(int i=0;i<bytesRead;i++){
                            dest2[i]=buf[i];
                        }
                        viceWriter.write(dest2);
                    } else {
                        viceWriter.write(buf);
                    }
                }
                if (this.size) {
                    viceWriter.setLength(file.length());
                }
                viceWriter.close();
            } else if (this.size) {
                ViceWriter viceWriter = venus.getsrvVice().upload(this.fileName, this.mode);
                viceWriter.setLength(file.length());
                viceWriter.close();
            }
        }
        file.close();
    }

    private RandomAccessFile copiarDeCache(Venus venus, String fileName, String mode) throws IOException, RemoteException, FileNotFoundException{
        ViceReader viceReader = venus.getsrvVice().download(fileName, mode);
        RandomAccessFile fichero = new RandomAccessFile(cacheDir + fileName, "rw");
        int blockSize = Integer.parseInt(venus.getTam());
        byte[] buf;
        while ((buf = viceReader.read(blockSize)) != null) {
            fichero.write(buf);
        }
        fichero.close();
        viceReader.close();
        return new RandomAccessFile(cacheDir + fileName, mode);
        
    }

    private boolean existeEnCache(String filename) {
        File f = new File(cacheDir);
        File[] files = f.listFiles();
        for (File file : files) {
            if (file.getName().equals(filename)) {
                return true;
            }
        }
        return false;
    }
}

