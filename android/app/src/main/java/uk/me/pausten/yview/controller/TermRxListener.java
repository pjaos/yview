package uk.me.pausten.yview.controller;

/**
 * \brief An interface to provide notification of the arrival of data from the WyTerm unit
 */
public interface TermRxListener {
    /*
    * @brief When data is received from the WyTerm unit this method is called to process the data.
    * @param rxBuffer      A byte array containing the data received on the socket fronm the WyTerm unit.
    * @param rxByteCount   The number of bytes received.
    */
    void processData(byte rxBuffer[], int rxByteCount);
}
