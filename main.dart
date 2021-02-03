// Copyright 2018 The Flutter team. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:english_words/english_words.dart';
//import 'package:socket_io/socket_io.dart';
import 'package:socket_io_client/socket_io_client.dart' as IO;

void main() {
  runApp(MaterialApp(
    home: Home(),
  ));
}

class Home extends StatelessWidget {
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.blueGrey,
      body: Center(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Image(
              image: AssetImage('images/logo.png')),
            ElevatedButton(
              style: ButtonStyle(
                  foregroundColor: MaterialStateProperty.all<Color>(Colors.black),
                  backgroundColor:
                      MaterialStateProperty.all<Color>(Colors.white)),
              child: Text('Sign in'),
              onPressed: () {
                IO.Socket socket = IO.io('http://192.168.0.142:4567/');
                socket.onConnect((_) {
                  print('connect');
                  socket.emit('msg', 'test');
                });
                Navigator.of(context).push(_createRoute(SignIn()));
              },
            ),
            TextButton(
              style: ButtonStyle(
                  foregroundColor:
                      MaterialStateProperty.all<Color>(Colors.white)),
              child: Text('Register user'),
              onPressed: () {
                // Funksjonalitet
              },
            )
          ],
        ),
      ),
    );
  }
}

Route _createRoute(page) {
  return PageRouteBuilder(
    pageBuilder: (context, animation, secondaryAnimation) => page,
    transitionsBuilder: (context, animation, secondaryAnimation, child) {
      return child;
    },
  );
}

class SignIn extends StatelessWidget {
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.blueGrey,
      body: Center(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            ElevatedButton(
              style: ButtonStyle(
                  foregroundColor: MaterialStateProperty.all<Color>(Colors.white),
                  backgroundColor:
                  MaterialStateProperty.all<Color>(Colors.blueAccent)),
              child: Text('Sign in with Google'),
              onPressed: () {
                //Navigator.of(context).push(_createRoute());
              },
            ),
            Text('Or'),
            ElevatedButton(
              style: ButtonStyle(
                  backgroundColor:
                  MaterialStateProperty.all<Color>(Colors.white),
                  foregroundColor:
                  MaterialStateProperty.all<Color>(Colors.black)),
              child: Text('Register user'),
              onPressed: () {
                // Funksjonalitet
              },
            ),
            ElevatedButton(
              style: ButtonStyle(
                  backgroundColor:
                  MaterialStateProperty.all<Color>(Colors.white),
                  foregroundColor:
                  MaterialStateProperty.all<Color>(Colors.blueGrey)),
              child: Text('Back'),
              onPressed: () {
                // Funksjonalitet
              },
            )
          ],
        ),
      ),
    );
  }
}