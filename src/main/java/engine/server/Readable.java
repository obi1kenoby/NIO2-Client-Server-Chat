package engine.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;


public class Readable implements Runnable {

    private ServerSocketChannel channel;
    private Selector selector;
    private ByteBuffer buffer;

    public Readable(ServerSocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            System.out.println("selector can not be opened!");
        }

        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        accept(key);
                    }
                    if (key.isReadable()) {
                        read(key);
                    }
                }
            } catch (IOException e) {
                System.out.println("to get a selection keys is impossible!");
            }
        }
    }

    private void read(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        StringBuilder builder = new StringBuilder();
        buffer = ByteBuffer.allocate(256);
        int read = 0;
        try {
            while ((read = socketChannel.read(buffer)) > 0) {
                buffer.flip();
                byte[] bytes = new byte[buffer.limit()];
                buffer.get(bytes);
                builder.append(new String(bytes));
                buffer.clear();
            }
            String message = builder.toString();
            if (read < 0){
                message = key.attachment() + " has left us.";
                System.out.println(message);
                broadcast(message);
                socketChannel.close();
            }
            else {
                if (key.attachment() == null && !message.equals("")){
                    key.attach(message);
                }
                else {
                    System.out.println(message);
                    broadcast(message);
                }
            }
        } catch (IOException e) {
            System.out.println("connection with client was terminated...");
        }
    }

    private void accept(SelectionKey key) {
        try {
            SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
            String address = socketChannel.socket().getInetAddress().toString();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, key.OP_READ);
            System.out.println("successfully connection from:" + address);
        } catch (IOException e) {
            System.out.println("connection to the client can not be established!!!");
        }
    }

    private void broadcast(String message){
        buffer = ByteBuffer.wrap(message.getBytes());
        try {
            for (SelectionKey key : selector.keys()) {
                if (key.isValid() && key.channel() instanceof SocketChannel) {
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    socketChannel.write(buffer);
                    buffer.rewind();
                }
            }
        }
        catch (IOException e){
            System.out.println("can not write message to clients");
        }
    }
}
