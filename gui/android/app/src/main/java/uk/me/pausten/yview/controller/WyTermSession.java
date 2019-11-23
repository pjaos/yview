package uk.me.pausten.yview.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

import uk.me.pausten.yview.model.Constants;
import uk.me.pausten.yview.view.MainActivity;

/**
 * A terminal session, consisting of a connection to the WyTerm unit and a terminal emulator that
 * is responsible for displaying text from a WyTerm unit and sending user entered text to the
 * Wyterm unit.
 */
public class WyTermSession extends Thread {
    Socket wyTermSocket;
    InputStream socketInputStream;
    OutputStream socketOutputStream;
    byte wyTermRxBuffer[] = new byte[Constants.YTERM_RX_BUFFER_SIZE];
    int rxByteCount;
    String wyTermAddress;
    int wyTermPort;
    Vector<TermRxListener> termRxListenerList = new Vector<>();

    /**
     * Set the Wyterm units IP address.
     *
     * @param wyTermAddress The IP address of the WyTerm unit.
     */
    public void setWyTermAddress(String wyTermAddress) {
        String array[] = wyTermAddress.split(":");
        if( array.length == 2 ) {
            this.wyTermAddress = array[0];
            try {
                wyTermPort = -1;
                wyTermPort = Integer.parseInt(array[1]);
            }
            catch( NumberFormatException e) {}
        }
        else {
            this.wyTermAddress = wyTermAddress;
            wyTermPort =  Constants.WYTERM_TCP_PORT;
        }

    }

    /**
     * Thread responsible for reading from the TCPIP connection connected to the WyTerm unit.
     */
    public void run() {
        rxByteCount=0;
        if( wyTermPort == -1 ) {
            return;
        }
        try {
            wyTermSocket = new Socket(wyTermAddress, wyTermPort);

            socketInputStream = wyTermSocket.getInputStream();

            socketOutputStream = wyTermSocket.getOutputStream();

            //Keep listening on the socket while it is connected
            while ( wyTermSocket != null && !wyTermSocket.isClosed() && rxByteCount >= 0 ) {

                rxByteCount = socketInputStream.read(wyTermRxBuffer);
                MainActivity.Log("WyTermSession rxByteCount = "+rxByteCount );

                if (rxByteCount > 0 && termRxListenerList.size() > 0) {
                    for (TermRxListener termRxListener : termRxListenerList) {
                        MainActivity.Log("WyTermSession RX data: "+ new String(wyTermRxBuffer, 0 , rxByteCount) );
                        termRxListener.processData(wyTermRxBuffer, rxByteCount);
                    }
                }

            }

        }
        //Normal socket close
        catch (SocketException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        shutDown();
    }

    /**
     * @brief Determone if we are connected to the remote device.
     * @return True if connected, false if not.
     */
    public boolean isConnected() {
        boolean closed=true;
        if( wyTermSocket != null ) {
            closed = wyTermSocket.isClosed();
        }
        return !closed;
    }

    /**
     * Shutdown the terminal session.
     */
    public void shutDown() {
        if (wyTermSocket != null) {
            try {
                wyTermSocket.close();
                socketInputStream = null;
                socketOutputStream = null;
                wyTermSocket = null;
            } catch (IOException e) {
            }
        }
    }

    /**
     * Send data to the WyTerm unit
     *
     * @param txData The dat ato be sent to the WyTerm unit.
     */
    public void sendData(byte txData[]) throws IOException {
        MainActivity.Log("WyTermSession.sendData("+new String(txData)+")" );
        if( socketOutputStream != null ) {
            //We send data in a 'worker' thread as IO should not occur in the GUI thread.
            class TXDataThread extends Thread {
                OutputStream socketOutputStream;
                byte txData[];
                public TXDataThread(OutputStream socketOutputStream, byte txData[]) {
                    this.socketOutputStream=socketOutputStream;
                    this.txData=txData;
                }
                public synchronized void run() {
                    try {
                        socketOutputStream.write(txData);
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            TXDataThread txDataThread = new TXDataThread(socketOutputStream, txData);
            txDataThread.start();
        }
    }

    /**
     * \brief Add to the list of termRxReceiver objects.
     * \param termRxListener The object to be notified of data received on the terminal.
     */
    public void addTermRxListener(TermRxListener termRxListener) {
        termRxListenerList.add(termRxListener);
    }

    /**
     * \brief Remove from the list of termRxReceiver objects.
     * \param termRxListener The object to be notified of data received on the terminal.
     */
    public void removeTermRxListener(TermRxListener termRxListener) {
        termRxListenerList.remove(termRxListener);
    }

    /**
     * \brief Remove all termRxReceiver objects
     */
    public void removeAllTermRxListener() {
        termRxListenerList.removeAllElements();
    }

}
