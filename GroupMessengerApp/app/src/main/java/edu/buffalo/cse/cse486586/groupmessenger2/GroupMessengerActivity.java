package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";

    static final int SERVER_PORT = 10000;
    int msg_count = 0;
    int msg_no = 0;
    Double proposed = 0.0;
    int agreed = 0;
    Double accepted = 0.0;
    boolean messageFlag = true;
    boolean proposalFlag = false;
    String messageToRead = null;
    String clientPort = null;
    String serverPort = null;
    String msgNumber = null;
    String Qmessage = null;
    int portNOwithPrpsal = 0;
    Double msgUniqueId=0.0;
    Double propUnique=0.0;
    int messageID;
    String status = "false";
    String FailedPort=null;
    boolean portFailed=false;
    List<String> acceptedList = new ArrayList<String>();
    PriorityBlockingQueue<String> holdBackQ = new PriorityBlockingQueue<String>(150, new PropoasalOrder());
    Map<Integer, String> mIdMvalue = new HashMap<Integer, String>();
    PriorityBlockingQueue<String> deliveryQ = new PriorityBlockingQueue<String>(50, new PropoasalOrder());
    //PriorityQueue<Map<Integer,String>> holdBackQ = new PriorityQueue<Map<Integer,String>>(50,new PropoasalOrder());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
       /* TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
*/


        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());


        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button button = (Button) findViewById(R.id.button4);
        final TextView localTv = (TextView) findViewById(R.id.local_text_display);
        final TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
        button.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.

                localTv.append(msg);
                remoteTextView.append("\n");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }

        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    public boolean isDeliverable(PriorityBlockingQueue<String> holdBackQ ){
        if((holdBackQ.peek().split("-")[5]).equals("true")){
            return  true;
        }
        else return false;
    }

    /*Delete all the messages that are sent by the failed process and are still in the queue*/
   void  cleanUptheQueue(){

       if(FailedPort!=null) {
           Iterator<String> failQitr = holdBackQ.iterator();
           while (failQitr.hasNext()) {
               String failStr = failQitr.next();
               String[] Failstrsplit = failStr.split("-");
               if ((FailedPort).equals(Failstrsplit[1])) {
                   holdBackQ.remove(failStr);
               }
           }
       }
   }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {


        @Override
        protected synchronized Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            String message;

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */


            try {

                while (true) {
                    Socket socket = serverSocket.accept();


                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    message = (String) input.readUTF();

                    String[] full_message = message.split("-");

                    DataOutputStream output = null;
                    Iterator<String> Delqitr = holdBackQ.iterator();
                    String failedPort=null;
                    /*Send the proposal to the process which sent the message
                     and add the message and all the information about it to the priorityQueue */
                    if (full_message.length > 4) {
                        proposed = proposed+ 1;
                        messageToRead = full_message[0];
                        clientPort = full_message[1];
                        serverPort = full_message[2];
                        msgNumber = full_message[3];
                        // status = "false";
                        int propInt= proposed.intValue();
                        String propUnique=propInt+"."+ serverPort;
                        Double propUniqueDouble =Double.valueOf(propUnique);
                        msgUniqueId = Double.valueOf( msgNumber + "." + clientPort);
                        Qmessage = messageToRead + "-" + clientPort + "-" + serverPort + "-" + msgUniqueId + "-" + propUniqueDouble + "-" + "false";


                            output = new DataOutputStream(socket.getOutputStream());
                            output.writeUTF(propUniqueDouble + "-" + msgUniqueId);

                        holdBackQ.add(Qmessage);
                        output.flush();

                    } else if (full_message.length == 2) {
                        /*Agreed sequence number is received ,
                         old proposal of the message in the queue is updated and marked true to deliver  */
                        output = new DataOutputStream(socket.getOutputStream());
                        output.writeUTF(serverPort + "-" + proposed);
                        accepted = Double.valueOf(full_message[0]);

                        proposed = Math.max(proposed, accepted);

                        Iterator<String> qitr = holdBackQ.iterator();
                        while (qitr.hasNext()) {
                            String str = qitr.next();
                            String[] strsplit = str.split("-");
                            int mId = Integer.valueOf(str.split("-")[2]);
                            //Log.e("IDMatch", (strsplit[1] + "-" + strsplit[3]) + ":" + full_message[2] + "-" + full_message[3]);
                            if (( strsplit[3]).equals(full_message[1])) {

                                strsplit[4] = Double.toString(accepted);

                                String strnew = strsplit[0] + "-" + strsplit[1] + "-" + strsplit[2] + "-" + strsplit[3] + "-" + strsplit[4] + "-" + "true";

                                holdBackQ.add(strnew);
                                holdBackQ.remove(str);
                                //  break;
                                output.flush();
                            }

                        }

                    }
                   // Thread.sleep(200);
                    /*delete the messages in the queue which are sent by the failed process*/
                    cleanUptheQueue();
                    /*Deliver the  messages if marked true for delivery and are at the head of the queue*/
                    Thread.sleep(300);
                    while (isDeliverable(holdBackQ)) {
                        publishProgress(holdBackQ.poll().split("-")[0]);

                        if (holdBackQ.isEmpty()) break;
                    }


                    socket.close();

                }


            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            //*References used
            //https://studylib.net/doc/7830646/isis-algorithm-for-total-ordering-of-messages
            //*https://developer.android.com/reference/android/os/AsyncTask
            //*https://developer.android.com/reference/java/io/DataInputStream and https://developer.android.com/reference/java/io/DataOutputStream
            //*https://docs.oracle.com/javase/tutorial/networking/sockets
            //*https://stackoverflow.com/questions/28187038/tcp-client-server-program-datainputstream-dataoutputstream-issue

            return null;
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            String remoteMessage = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
            remoteTextView.append(remoteMessage + "\n");

            TextView localTextView = (TextView) findViewById(R.id.local_text_display);
            localTextView.append("\n");

            String scheme = "content";
            String authority = "edu.buffalo.cse.cse486586.groupmessenger2.provider";
            Uri myUri = buildUri(scheme, authority);

            String[] ColumnNames = {"key", "value"};
            /* References-https://developer.android.com/guide/topics/providers/content-provider-basics#Inserting*/
            /*Defining a ContentValues object to insert a new row which takes column name and value as its input
             * Here, column name "key" will have message counter as its value.
             * and column name "value" will have message itself
             */
            ContentValues contVal = new ContentValues();
            contVal.put(ColumnNames[0], Integer.toString(msg_count++));
            contVal.put(ColumnNames[1], remoteMessage);
            /* inserting a new row into the provider and which returns a content URI for that row.
             * getContentResolver() calls an instance of the class which implements contentProvider.
             * */
            Uri mUri = getContentResolver().insert(myUri, contVal);


        }

    }
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected synchronized Void doInBackground(String... msgs) {

                List<String> remotePorts = new ArrayList<String>();
                remotePorts.add(REMOTE_PORT0);
                remotePorts.add(REMOTE_PORT1);
                remotePorts.add(REMOTE_PORT2);
                remotePorts.add(REMOTE_PORT3);
                remotePorts.add(REMOTE_PORT4);
                if(portFailed==true){
                    remotePorts.remove(FailedPort);
                }
                // Socket socket=null;
                int maxprop=0;
                msg_no++;

                String msgID=null;
                List<Double> propList = new ArrayList<Double>();
                /*Creating a new socket connections for each AVDs and multi-casting message to all of them and to itself */
                for(String remotePort :remotePorts ) {
                    if(remotePort.equals(FailedPort)) continue;
                    try {
                        messageFlag = true;
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));

                        /*Send the message and all the data pertaining to it */

                        String msgToSend = msgs[0] + "-" + msgs[1] + "-" + remotePort + "-" + msg_no + "-" + "redundant";

                        String message = null;
                        /*
                         * TODO: Fill in your client code that sends out a message.
                         */


                        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                        output.writeUTF(msgToSend);

                        /*Read the proposal sent by all the processes put add them to a List*/
                            DataInputStream input = new DataInputStream(socket.getInputStream());
                            message = (String) input.readUTF();


                        String[] proposal = message.split("-");
                        msgID = proposal[1];
                        propList.add(Double.valueOf(proposal[0]));

                        /*References used*/
                        /*https://developer.android.com/reference/android/os/AsyncTask*/
                        /*https://developer.android.com/reference/java/io/DataInputStream and https://developer.android.com/reference/java/io/DataOutputStream*/
                        /*https://docs.oracle.com/javase/tutorial/networking/sockets/*/
                        /*https://stackoverflow.com/questions/28187038/tcp-client-server-program-datainputstream-dataoutputstream-issue*/

                        //messageFlag=false;
                        output.flush();
                        socket.close();
                    } catch (IOException e) {
                        Log.e("FailedPort",remotePort);
                        /*assigned the failed port and make it visible throughout the code*/
                             FailedPort =remotePort;
                            portFailed=true;

                        }
                }
                /*Take max of all proposals and multi-cast to all processes*/
                Double acceptedPortandValue=Collections.max(propList);

                for(int j=0;j<remotePorts.size();j++) {
                    if (remotePorts.get(j) == FailedPort) continue;
                    try {
                        proposalFlag = true;
                        // messageFlag=false;
                        Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePorts.get(j)));

                        DataOutputStream output1 = new DataOutputStream(socket1.getOutputStream());
                        output1.writeUTF(acceptedPortandValue + "-" + msgID);
                        DataInputStream input = new DataInputStream(socket1.getInputStream());
                        String message = (String) input.readUTF();

                        //*References used
                        //*https://developer.android.com/reference/android/os/AsyncTask
                        //*https://developer.android.com/reference/java/io/DataInputStream and https://developer.android.com/reference/java/io/DataOutputStream
                        //*https://docs.oracle.com/javase/tutorial/networking/sockets
                        //*https://stackoverflow.com/questions/28187038/tcp-client-server-program-datainputstream-dataoutputstream-issue


                        proposalFlag = false;
                        output1.flush();
                        socket1.close();
                    }catch (UnknownHostException e) {
                        FailedPort =remotePorts.get(j);
                        portFailed=true;

                    } catch (IOException e) {
                        FailedPort =remotePorts.get(j);
                        portFailed=true;
                    }

                }

            return null;
        }


    }
}









