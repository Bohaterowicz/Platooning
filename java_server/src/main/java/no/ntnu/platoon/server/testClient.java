package no.ntnu.platoon.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class testClient {
    public static void main(String[] args) throws IOException {
        Socket s = new Socket("158.38.101.173", 4567);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        System.out.println("ASD");
    }
}
