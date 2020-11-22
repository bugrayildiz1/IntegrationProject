import client.*;
import org.w3c.dom.Node;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This is just some example code to show you how to interact
 * with the server using the provided client and two queues.
 * Feel free to modify this code in any way you like!
 */

public class MyProtocol{

    // The host to connect to. Set this to localhost when using the audio interface tool.
    private static String SERVER_IP = "netsys2.ewi.utwente.nl"; //"127.0.0.1";
    // The port to connect to. 8954 for the simulation server.
    private static int SERVER_PORT = 8954;
    // The frequency to use.
    private static int frequency = 18100;

    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;
    int NodeID = (int) (Math.random() * (124 - 1)) + 1;
    byte destination;
    boolean repeat;
    int packeTsent=0;
    int concatMid=-1;
    boolean free=true;
    HashMap<Byte, Integer> nodeTable = new HashMap<>();
    HashMap<Byte, Byte> routingTable = new HashMap<>();
    HashMap<Byte, Integer> messageTrack = new HashMap<>();
    LinkedHashMap<String, byte[]> ackList = new LinkedHashMap<String, byte[]>();
    public MyProtocol(String server_ip, int server_port, int frequency){

        receivedQueue = new LinkedBlockingQueue<>();
        sendingQueue = new LinkedBlockingQueue<>();

        new Client(SERVER_IP, SERVER_PORT, frequency, receivedQueue, sendingQueue); // Give the client the Queues to use

        new receiveThread(receivedQueue).start(); // Start thread to handle received messages!

        // handle sending from stdin from this thread.
        try{
            ByteBuffer temp = ByteBuffer.allocate(1024);
            int read;
            while(true){
                read = System.in.read(temp.array()); // Get data from stdin, hit enter to send!
                if(read > 0 && read < 26){
                    byte[] arr2 = Arrays.copyOfRange(temp.array(), 0, 32);
                    packeTsent++;
                    arr2[26] = (byte)0;
                    arr2[27] = (byte) packeTsent;
                    arr2[28]= (byte)NodeID;
                    arr2[29]= (byte)0;
                    arr2[30] = (byte)0;
                    arr2[31] = (byte)read;
                    ByteBuffer toSend = ByteBuffer.wrap(arr2);
                    Message msg = new Message(MessageType.DATA, toSend);
                    int backoff=0;
                    while(true){
                        Thread.sleep(((int) (Math.random() * (2^backoff))));
                        backoff++;
                        if (receivedQueue.isEmpty() || free) {
                            for ( int key : messageTrack.keySet() ) {
                                ackList.put(concat(NodeID,concatMid,packeTsent,concatMid,key),arr2);
                            }
                            //    ackList.forEach((key, value) -> System.out.println("\n ACK: " + key + " Array:  " + Arrays.toString(value)));
                            sendingQueue.put(msg);
                            //System.out.print("Backoff " + backoff);
                            break;
                        }
                        if (backoff==3){
                            backoff=0;
                        }
                    }
                }

                if (read > 26){ //in the case that you are sending multiple packets
                    System.out.println("more than 32");
                    for (int i=0; i <= read/26 ;i++){
                        byte[] arr2 = Arrays.copyOfRange(temp.array(), i*26, i*26+32);
                        packeTsent++;
                        arr2[26] = (byte)0;
                        arr2[27] = (byte) packeTsent;
                        arr2[28]= (byte)NodeID;
                        arr2[29]= (byte)0;
                        arr2[30] = (byte) ((byte)1+i*26);
                        arr2[31] = (byte)read;
                        if (i == read/26){
                            arr2[30]= (byte) -i;
                        }
                        ByteBuffer toSend = ByteBuffer.wrap(arr2);
                        Message msg = new Message(MessageType.DATA, toSend);
                        int backoff=0;
                        while(true){
                            Thread.sleep(((int) (Math.random() * (2^backoff))) * 52);
                            backoff++;
                            if (receivedQueue.isEmpty() || free) {
                                for ( int key : messageTrack.keySet() ) {
                                    ackList.put(concat(NodeID,concatMid,packeTsent,concatMid,key),arr2);
                                }
                                //     ackList.forEach((key, value) -> System.out.println("\n ACK: " + key + " Array:  " + Arrays.toString(value)));
                                sendingQueue.put(msg);
                                //System.out.print("Backoff " + backoff);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException | IOException e){
            System.exit(2);
        }
    }
    public String concat(int a, int b, int c,int d, int e){

        // Convert both the integers to string
        String s1 = Integer.toString(a);
        String s2 = Integer.toString(b);
        String s3 = Integer.toString(c);
        String s4 = Integer.toString(d);
        String s5 = Integer.toString(e);
        // Concatenate both strings

        // Convert the concatenated string
        // to integer


        // return the formed integer
        return s1 + s2 + s3 + s4 + s5;
    }
    public static void main(String[] args) {


        if(args.length > 0){
            frequency = Integer.parseInt(args[0]);
        }
        new MyProtocol(SERVER_IP, SERVER_PORT, frequency);
        // creating timer task, timer
        // creating timer task, timer
    }

    private class receiveThread extends Thread {
        private BlockingQueue<Message> receivedQueue;

        public receiveThread(BlockingQueue<Message> receivedQueue){
            super();
            this.receivedQueue = receivedQueue;
        }
        byte [] output;
        public void makeArray(int bytesLength){
            output= new byte[Math.abs(bytesLength)+1];
        }
        public void printByteBufferDATA(ByteBuffer bytes, int bytesLength){
            if(bytes.get(29)==NodeID || bytes.get(29)==0) {
                if(bytes.get(26) != 1){
                    if (messageTrack.get(bytes.get(28))+1==bytes.get(27)){
                        if (bytes.get(30) == (byte) 0) {
                            byte[] output = bytes.array();
                            String string = new String(output, StandardCharsets.UTF_8);
                            String modified = string.substring(0, bytesLength);
                            System.out.println(bytes.get(28) + "says: " + modified);
                        }
                        if (bytes.get(30) > (byte) 0) {
                            if (bytes.get(30) < (byte) 27) {
                                makeArray(bytesLength);
                            }
                            for (int i = -1; i < 26; i++) {
                                output[bytes.get(30) + i] = bytes.get(i + 1);
                            }
                        }
                        if (bytes.get(30) < (byte) 0) {
                            int remainder = Math.abs(bytesLength % 26);
                            for (int i = -1; i < remainder; i++) {
                                output[bytesLength - remainder + i + 1] = bytes.get(i + 1);
                            }
                            String string = new String(output, StandardCharsets.UTF_8);
                            System.out.println("Node ID " + bytes.get(28) + " says: " + string);
                        }
                    }
                }
            }
        }
        public void printByteBuffer(ByteBuffer bytes, int bytesLength){
            for(int i=0; i<bytesLength; i++){
                System.out.print(bytes.get(i) +" " );
            }
        }

        private void routeCollection(ByteBuffer bytes, int NodeID) throws InterruptedException {
            if(bytes.get(0)==(byte)-125 && bytes.get(1)==-(byte)125){
                nodeTable.clear();
                routingTable.clear();
                ByteBuffer setup = ByteBuffer.allocate(2);
                setup.put(0, bytes.get(0));
                setup.put(0, bytes.get(0));
                Message msg = new Message(MessageType.DATA_SHORT, setup);
                int backoff = 2;
                while (true) {
                    Message m = receivedQueue.peek();
                    Thread.sleep(((int) (Math.random() * (Math.pow(2, backoff)))));
                    backoff++;
                    if ((receivedQueue.isEmpty() || free)) {
                        sendingQueue.put(msg);
                        //   System.out.print("Backoff " + backoff);
                        break;
                    }
                    if (backoff > 11) {
                        backoff = (int) (Math.random() * (10 - 1)) + 1;
                        if (backoff > 6) {
                            free = true;
                        }
                    }
                }
            }
            nodeTable.put((byte) NodeID, 0);
            if (bytes.get(0) > 0) {
                if ((bytes.get(1) == (byte) 0) && (bytes.get(0) > 0)) {
                    ByteBuffer setup = ByteBuffer.allocate(2);
                    setup.put(0, bytes.get(0));
                    nodeTable.put((bytes.get(0)), 1);
                    messageTrack.put((bytes.get(0)), 0);
                    setup.put(1, (byte) NodeID);
                    Message msg = new Message(MessageType.DATA_SHORT, setup);
                    int backoff = 2;
                    while (true) {
                        Message m = receivedQueue.peek();
                        Thread.sleep(((int) (Math.random() * (Math.pow(2, backoff)))));
                        backoff++;
                        if ((receivedQueue.isEmpty() || free)) {
                            sendingQueue.put(msg);
                            //   System.out.print("Backoff " + backoff);
                            break;
                        }
                        if (backoff > 11) {
                            backoff = (int) (Math.random() * (10 - 1)) + 1;
                            if (backoff > 6) {
                                free = true;
                            }
                        }
                    }
                }
                if (bytes.get(0) == (byte) NodeID && !nodeTable.containsKey(bytes.get(1))) {
                    if (bytes.get(1) > 0) {
                        nodeTable.put((bytes.get(1)), 1);
                        messageTrack.put((bytes.get(1)), 0);
                    }
                }
                if (nodeTable.containsKey(bytes.get(1)) && !nodeTable.containsKey(bytes.get(0))) {
                    //  nodeTable.put(bytes.get(0),nodeTable.get(bytes.get(1))+1);
                    if (bytes.get(0) > 0 && bytes.get(1) > 0) {
                        messageTrack.put((bytes.get(0)), 0);
                        ByteBuffer unreachable = ByteBuffer.allocate(2);
                        unreachable.put(0, (byte) (-1 * (bytes.get(0))));
                        unreachable.put(1, (byte) NodeID);
                        Message msg = new Message(MessageType.DATA_SHORT, unreachable);
                        int backoff = 2;
                        while (true) {
                            Message m = receivedQueue.peek();
                            Thread.sleep(((int) (Math.random() * (Math.pow(2, backoff)))));
                            backoff++;
                            if ((receivedQueue.isEmpty() || free)) {
                                sendingQueue.put(msg);
                                //   System.out.print("Backoff " + backoff);
                                break;
                            }
                            if (backoff > 11) {
                                backoff = (int) (Math.random() * (10 - 1)) + 1;
                                if (backoff > 6) {
                                    free = true;
                                }
                            }
                        }
                    }
                }
            }
            if (nodeTable.containsKey((byte) (-1 * bytes.get(0))) && nodeTable.containsKey(bytes.get(1))) {
                routingTable.put((byte) (-1 * bytes.get(0)), bytes.get(1));
                ByteBuffer unreachable = ByteBuffer.allocate(2);
                unreachable.put(0, ((byte) (-1 * (bytes.get(0)))));
                unreachable.put(1, (byte) (-1 * NodeID));
                Message msg = new Message(MessageType.DATA_SHORT, unreachable);
                int backoff = 2;
                while (true) {
                    Message m = receivedQueue.peek();
                    Thread.sleep(((int) (Math.random() * (Math.pow(2, backoff)))));
                    backoff++;
                    if ((receivedQueue.isEmpty() || free)) {
                        sendingQueue.put(msg);
                        //  System.out.print("Backoff " + backoff);
                        break;
                    }
                    if (backoff > 9) {
                        backoff = (int) (Math.random() * (10 - 1)) + 1;
                        if (backoff > 6) {
                            free = true;
                        }
                    }
                }

            }
            if (nodeTable.containsKey((byte) (-1 * bytes.get(0))) && !nodeTable.containsKey(bytes.get(1))) {
                ByteBuffer unreachable = ByteBuffer.allocate(2);
                unreachable.put(0, (byte) (-1 * (bytes.get(1))));
                unreachable.put(1, (byte) NodeID);
                Message msg = new Message(MessageType.DATA_SHORT, unreachable);
                int backoff = 2;
                while (true) {
                    Message m = receivedQueue.peek();
                    Thread.sleep(((int) (Math.random() * (Math.pow(2, backoff)))));
                    backoff++;
                    if ((receivedQueue.isEmpty() || free)) {
                        sendingQueue.put(msg);
                        //     System.out.print("Backoff " + backoff);
                        break;
                    }
                    if (backoff > 10) {
                        backoff = (int) (Math.random() * (10 - 1)) + 1;
                        if (backoff > 6) {
                            free = true;
                        }
                    }
                }

            }

            if ((nodeTable.containsKey((byte) (-1 * bytes.get(1))) && !nodeTable.containsKey(bytes.get(0)))) {
                ByteBuffer unreachable = ByteBuffer.allocate(2);
                unreachable.put(0, (byte) (-1 * (bytes.get(0))));
                unreachable.put(1, (byte) NodeID);
                Message msg = new Message(MessageType.DATA_SHORT, unreachable);
                int backoff = 2;
                while (true) {
                    Message m = receivedQueue.peek();
                    Thread.sleep(((int) (Math.random() * (Math.pow(2, backoff)))));
                    backoff++;
                    if ((receivedQueue.isEmpty() || free)) {
                        sendingQueue.put(msg);
                        //   System.out.print("Backoff " + backoff);
                        break;
                    }
                    if (backoff > 10) {
                        backoff = (int) (Math.random() * (10 - 1)) + 1;
                        if (backoff > 6) {
                            free = true;
                        }
                    }
                }
            }


            //  nodeTable.forEach((key, value) -> System.out.println("\n Destination: " + key + " Hop:  " + value));
        }




        public void messagetrack(ByteBuffer bytes){
            if(messageTrack.containsKey(bytes.get(28)) && messageTrack.get(bytes.get(28))<bytes.get(27)){
                messageTrack.put(bytes.get(28), messageTrack.get(bytes.get(28)) + 1);
                //  messageTrack.forEach((key, value) -> System.out.println("\n NodeID " + key + " Count:  " + value));
            }
        }
        public void repeat(ByteBuffer temp, byte destination) {
            if ((temp.get(29)==NodeID || temp.get(29)==0)&&(temp.get(28)!=NodeID)){
                if (messageTrack.get(temp.get(28))+1==temp.get(27)) {
                    byte[] arr2 = Arrays.copyOfRange(temp.array(), 0, 32);
                    arr2[29] = destination;
                    ByteBuffer toSend = ByteBuffer.wrap(arr2);
                    Message msg;
                    msg = new Message(MessageType.DATA, toSend);
                    try {
                        if (free) {
                            Thread.sleep((long) (Math.random() * 7000));
                            sendingQueue.put(msg);
                            ackList.put(concat(arr2[29],concatMid,packeTsent,concatMid,destination),arr2);
                            //           ackList.forEach((key, value) -> System.out.println("\n ACK: " + key + " Array:  " + value));
                        } else {
                            Thread.sleep((long) (Math.random() * 14000));
                            sendingQueue.put(msg);
                            ackList.put(concat(arr2[29],concatMid,packeTsent,concatMid,destination),arr2);
                            //      ackList.forEach((key, value) -> System.out.println("\n ACK: " + key + " Array:  " + value));
                        }
                        sendingQueue.put(msg);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void ackCheck(ByteBuffer m){
            if (m.get(26)==1 && ackList.containsKey( concat(m.get(28),concatMid,m.get(27),concatMid,m.get(25)))){
                ackList.remove(concat(m.get(28),concatMid,m.get(27),concatMid,m.get(25)));
                // ackList.forEach((key, value) -> System.out.println("\n ACK: " + key + " Array:  " + value));
                if(ackList.isEmpty()){
                    System.out.println("The ACK list is empty");
                }
            }
        }
        public void ackSend(ByteBuffer m) throws InterruptedException {
            if (m.get(26)!=1){
                byte[] arr2 = Arrays.copyOfRange(m.array(), 0, 32);
                arr2[26] = 1;
                arr2[25] = (byte) NodeID;
                ByteBuffer toSend = ByteBuffer.wrap(arr2);
                Message msg;
                msg = new Message(MessageType.DATA, toSend);
                int backoff=2;
                while(true){
                    Thread.sleep(((int) (Math.random() * (Math.pow(2,backoff)))));
                    backoff++;
                    if ((receivedQueue.isEmpty() || free)) {
                        sendingQueue.put(msg);
                        //System.out.print("Backoff " + backoff);
                        break;
                    }
                    if (backoff>10) {
                        backoff = (int) (Math.random() * (10 - 1)) + 1;
                        if (backoff > 6) {
                            free = true;
                        }
                    }
                }

            }
        }
        public void retransmit() throws InterruptedException {
            String first = ackList.keySet().iterator().next();
            byte[] arr2 = Arrays.copyOfRange(ackList.get(first), 0, 32);
            ackList.remove(first);
            ByteBuffer toSend = ByteBuffer.wrap(arr2);
            Message msg;
            msg = new Message(MessageType.DATA, toSend);
            int backoff=2;
            while(true){
                Message m = receivedQueue.peek();
                Thread.sleep(((int) (Math.random() * (Math.pow(2,backoff)))));
                backoff++;
                if ((receivedQueue.isEmpty() || free)) {
                    sendingQueue.put(msg);
                    //    System.out.println("Retransmission timeeeee!!");
                    //System.out.print("Backoff " + backoff);
                    break;
                }
                if (backoff>10) {
                    backoff = (int) (Math.random() * (10 - 1)) + 1;
                    if (backoff > 6) {
                        free = true;
                    }
                }
            }
        }



        public void run(){
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Timer timer = new Timer();
            TimerTask gorev = new TimerTask() {
                @Override
                public void run() {
                    try{

                        if (ackList.size() >0){
                            retransmit();
                            //    System.out.println("MESSAGE RETRANSMITTED");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            timer.schedule(gorev,0,9000);

            Timer timer2 = new Timer();
            TimerTask gorev2 = new TimerTask() {
                @Override
                public void run() {
                    messageTrack.forEach((key, value) -> System.out.println("The online node is " + key));
                }
            };
            timer2.schedule(gorev2,0,45000);

            ByteBuffer setup= ByteBuffer.allocate(2);
            setup.put(0,(byte)NodeID);
            System.out.println("Setup might take some time please wait approximately 30 seconds");
            Message msg = new Message(MessageType.DATA_SHORT, setup);
            try {
                sendingQueue.put(msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while(true) {
                try{
                    Message m = receivedQueue.take();
                    if (m.getType() == MessageType.BUSY){
                        free=false;
                        System.out.println("BUSY");
                    } else if (m.getType() == MessageType.FREE){
                        System.out.println("FREE");
                        free=true;
                    } else if (m.getType() == MessageType.DATA){
//                        System.out.print("DATA: ");
                        printByteBufferDATA( m.getData(), m.getData().get(31));//Just print the data
                        if (routingTable.containsKey(m.getData().get(28)) && (m.getData().get(29))==0){
                            destination= (routingTable.get(m.getData().get(28)));
                            repeat(m.getData(),destination);
                        }
                        ackSend(m.getData());
                        ackCheck(m.getData());
                        messagetrack(m.getData());
                    } else if (m.getType() == MessageType.DATA_SHORT){
//                        System.out.print("DATA_SHORT: ");
                        //      printByteBuffer( m.getData(), m.getData().capacity()); //Just print the data
                        routeCollection (m.getData(),NodeID);
                        Timer timer3= new Timer();
                        TimerTask gorev3 = new TimerTask() {
                            @Override
                            public void run() {
                                ByteBuffer setup= ByteBuffer.allocate(2);
                                setup.put(0,(byte)NodeID);
                                System.out.println("Setup might take some time please wait approximately 30 seconds");
                                Message msg = new Message(MessageType.DATA_SHORT, setup);
                                try {
                                    sendingQueue.put(msg);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                            }
                        };
                        timer3.schedule(gorev3,0,120000);
                    } else if (m.getType() == MessageType.DONE_SENDING){
//                        System.out.println("Message Sent");
                    } else if (m.getType() == MessageType.HELLO){
                        System.out.println("HELLO");
                    } else if (m.getType() == MessageType.SENDING){
//                        System.out.println("SENDING");
                    } else if (m.getType() == MessageType.END){
                        System.out.println("END");
                        System.exit(0);
                    }
                } catch (InterruptedException e){
                    System.err.println("Failed to take from queue: "+e);
                }
            }
        }

    }

}