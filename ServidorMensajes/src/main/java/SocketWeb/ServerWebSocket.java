/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package SocketWeb;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ServerEndpoint("/mensajes")
public class ServerWebSocket {

    private static Set<Session> clients = Collections.synchronizedSet(new HashSet<>());
    private static Map<Session, String> userNames = new ConcurrentHashMap<>();
    private static List<String> listaProductos = new ArrayList<>();

    @OnOpen
    public void onOpen(Session session) {
        clients.add(session);
        enviarProductos();
    }

    @OnClose
    public void onClose(Session session) {
        String userName = userNames.get(session);
        clients.remove(session);
        userNames.remove(session);
        broadcastUserList();
        broadcastMessage("El usuario " + userName + " ha salido del chat.");
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if (!userNames.containsKey(session)) {
            userNames.put(session, message);
            broadcastUserList();
            broadcastMessage("El usuario " + message + " se ha unido al chat.");
        } else {

            boolean privado = false;
            for (Session client : clients) {
                if (message.startsWith("/" + userNames.get(client) + " ")) {
                    privado = true;
                    try {
                        String boora = "/" + userNames.get(client) + " ";
                        message = message.substring(boora.length());
                        String mensajePrivado = "PRIVADO " + (userNames.get(session)) + "->" + (userNames.get(client)) + ":";
                        mensajePrivado = mensajePrivado.concat(message);
                        session.getBasicRemote().sendText(mensajePrivado);
                        client.getBasicRemote().sendText(mensajePrivado);
                    } catch (IOException ex) {
                        Logger.getLogger(ServerWebSocket.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }
            if (!privado) {
                if (message.startsWith("{")) {
                    ;
                    listaProductos.add(message);
                    enviarProductos();

                } else {
                    broadcastMessage(userNames.get(session) + ": " + message);
                }

            }

        }
    }

    private void enviarProductos() {
        broadcastMessage("LIMPIAR");
        for (String listaProducto : listaProductos) {
            broadcastMessage(listaProducto);
        }
    }

    @OnError
    public void onError(Throwable e) {
        e.printStackTrace();
    }

    private void broadcastMessage(String message) {
        clients.forEach(client -> {
            try {
                client.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private String buildUserListMessage() {
        StringBuilder userListMsg = new StringBuilder("Usuarios conectados:");
        Iterator<String> iterator = userNames.values().iterator();
        while (iterator.hasNext()) {
            String userName = iterator.next();
            userListMsg.append(userName);
            if (iterator.hasNext()) {
                userListMsg.append(", ");
            }
        }
        return userListMsg.toString();
    }

    private void broadcastUserList() {
        String userListMsg = buildUserListMessage();
        broadcastMessage(userListMsg);
    }
}
