package com.servidor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {

    public static void main(String[] args) throws IOException {
        Socket s = null;
        ServerSocket ss = new ServerSocket(8080);
        Monitor monitor = new Monitor();


        System.out.println("Servidor escuchando.........");

        while(true){
            s = ss.accept();
            Hilo worker = new Hilo(s, monitor);
            worker.start();
        }
    }
    static class Hilo extends Thread{
        private Socket s = null;
        private ObjectInputStream ois = null;
        private ObjectOutputStream oos = null;
        private Monitor monitor = null;



        public Hilo(Socket socket, Monitor monitor){
            this.s = socket;
            this.monitor = monitor;
        }

        public void run(){
            System.out.println("Conexión recibida desde " + s.getInetAddress());

            try {
                ois = new ObjectInputStream((s.getInputStream()));
                oos = new ObjectOutputStream((s.getOutputStream()));

                DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                Date date = new Date();

                String nombreUsuario = (String) ois.readObject();
                String bienvenida = "Bienvenido, " + nombreUsuario;

                oos.writeObject(bienvenida + "\n");
                oos.writeObject(monitor.getAllMessages());
                oos.writeObject((monitor.posicionMensajes));

                System.out.println("Se le ha saludado y enviado los mensajes enviados con éxito a " + s.getInetAddress() + " también conocido como " + nombreUsuario);

                String opcion = null;
                boolean terminar = false;

                while (!terminar){
                    opcion = (String) ois.readObject();
                    switch (opcion){
                        case "bye":
                            terminar = true;
                            oos.writeObject("Goodbye");
                            break;

                        case "mensaje":
                            oos.writeObject("Escriba su mensaje");

                            String mensajeNuevo = (String) ois.readObject();
                            monitor.putMessage("<" + nombreUsuario + "> [" + dateFormat.format(date) + ":" + "00" + "] " + mensajeNuevo);
                            break;

                        case "recibir":
                            oos.writeObject("\n Historial de mensajes");

                            int posicion = (int) ois.readObject();
                            oos.writeObject(monitor.getAllMessages(posicion));
                            oos.writeObject(monitor.posicionMensajes);
                            break;

                        default:
                            oos.writeObject("ERROR: Opción invalida");

                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (oos != null){oos.close();}
                    if (ois != null){ois.close();}
                    if (s != null){s.close();}
                    System.out.println("Niño, se acabó lo que se daba.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public static class Monitor {
        private CopyOnWriteArrayList<String> mensajes = new CopyOnWriteArrayList<String>();
        int posicionMensajes = 0;

        public Monitor() {
            mensajes.add("Mensajes: ");
        }

        public synchronized String getAllMessages(){
            String mensajeConcatenado = "";
            for (int i = 0; i < mensajes.size(); i++) {
                mensajeConcatenado += mensajes.get(i) + "\n";
            }

            posicionMensajes = mensajes.size();

            return mensajeConcatenado;
        }

        public synchronized String getAllMessages(int posicion){
            String mensajeConcatenado = "";
            for (int i = posicion; i < mensajes.size(); i++) {
                mensajeConcatenado += mensajes.get(i) + "\n";
            }

            posicionMensajes = mensajes.size();

            return mensajeConcatenado;
        }

        public synchronized void putMessage(String mensaje) {
            mensajes.add(mensaje);
        }
    }
}