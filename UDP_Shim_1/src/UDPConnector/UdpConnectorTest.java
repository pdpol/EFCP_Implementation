/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPConnector;

import java.io.*;
import java.net.*;
import java.util.*;
/**
 *
 * @author jhorjus
 */
public class UdpConnectorTest {

    public static void main(String [ ] args)
    {
         
        System.out.println("** UdpConnector and EfcpConnector Test ** ");
        
        // Part 1: Test a UdpConnector pair.
        try
        {        
            UdpConnector connection1 = new UdpConnector(1181);
            UdpConnector connection2 = new UdpConnector(1182);
            System.out.println("\n\n*** UdpConnectorTest: Section 1  ***");
            // start listening thread.
            connection1.StartReceiveThread();
             
            // send a packet
            System.out.println("send 2 packets...");
            InetAddress localhost = InetAddress.getLocalHost();
            connection2.SetPeerAddress(localhost, 1181);
            connection2.Send("first_packet");      
            connection2.Send("second_packet"); 
            
            //pick it up.
            System.out.println("Try to receive twice...");
            List<byte[]> dataPacketsReceived = connection1.Receive(2000);
            for(byte[] data : dataPacketsReceived)
            {
                System.out.print("Test 1: Receive 1: ");
                System.out.print(new String(data));
                System.out.print("\n");
            }
            dataPacketsReceived = connection1.Receive(2000);
            for(byte[] data : dataPacketsReceived)
            {
                System.out.print("Test 1: Receive 2: ");
                System.out.print(new String(data));
                System.out.print("\n");
            }
            System.out.print("Finished.\n");
            
            connection1.StopReceiveThread();
            connection2.StopReceiveThread();
        }
        catch(Exception e)
        {
            System.out.println("Test 1 Error:" + e.getMessage());
        }
        
        
        
        //Part 2: Test the connection manager 
        try
        {     
            System.out.println("\n\n\n*** UdpConnectorTest: Section 2  ***");
            UdpConnectionManager connMgr = new UdpConnectionManager();
            int flow1 = connMgr.AllocateFlow(1183, 1184, InetAddress.getLocalHost());
            int flow2 = connMgr.AllocateFlow(1184, 1183, InetAddress.getLocalHost());
            
            for (int ii = 0; ii<20; ii++)
            {
                System.out.print("Test 2 Process Send "+ ii +".\n");
                connMgr.Send(flow1, "Test2_Packet_"+ii+".");
            }
            
            List<byte[]> dataPacketsReceived = connMgr.Receive(flow2);
            for(byte[] data : dataPacketsReceived)
            {
                System.out.print("Test 2 Process Receive 1: " + new String(data) + "\n");
            }
            
            /// First receive may not have been able to pick up all packets (not yet arrived)
            try {Thread.sleep(1000);} catch(InterruptedException ex) {}
            dataPacketsReceived = connMgr.Receive(flow2);
            for(byte[] data : dataPacketsReceived)
            {
                System.out.print("Test 2 Process Receive 2: " + new String(data) + "\n");
            }
            
            connMgr.DeallocateFlow(flow1);
            connMgr.DeallocateFlow(flow2);
        }
        catch(Exception e)
        {
            System.out.println("Test 2 Error:" + e.getMessage());
        }        
        
        
        //Part 3: Test the Connection Shaper
        try
        {     
            System.out.println("\n\n\n*** UdpConnectorTest: Section 3  ***");
            
            ConnectionShaper shapedConn1 = new ConnectionShaper(
                    new UdpConnector(1185),
                    600, // 600ms min delay
                    300, // up to 300ms additional random delay (jitter).
                    3333 // 33% packet loss
                    );
            shapedConn1.SetPeerAddress(InetAddress.getLocalHost(), 1186);
            
            ConnectionShaper shapedConn2 = new ConnectionShaper(
                    new UdpConnector(1186),
                    600, // 600ms min delay
                    300, // up to 300ms additional random delay (jitter).
                    3333 // 33% packet loss
                    );
            shapedConn2.StartReceiveThread();
            
            for (int ii = 0; ii<20; ii++)
            {
                System.out.print("Test3 Process Send "+ ii +".\n");
                shapedConn1.Send("Test3_Packet_"+ii+".");
            }
            
            System.out.print("Test 3 Process Receive 1: Shouldn't get anything.");
            List<byte[]> dataPacketsReceived = shapedConn2.Receive(600);
            for(byte[] data : dataPacketsReceived)
            {
                System.out.print("Test 3 Process Receive 1: " + new String(data) + "\n");
            }
            
            /// First receive may not have been able to pick up all packets (not yet arrived)
            try {Thread.sleep(1000);} catch(InterruptedException ex) {}
            System.out.print("Test 3 Process Receive 2: Should get missing and out of order packets.\n");
            dataPacketsReceived = shapedConn2.Receive(0);
            for(byte[] data : dataPacketsReceived)
            {
                System.out.print("Test 3 Process Receive 2: " + new String(data) + "\n");
            }
            
            shapedConn1.StopReceiveThread();
            shapedConn2.StopReceiveThread();
        }
        catch(Exception e)
        {
            System.out.println("Test3 Error:" + e.getMessage());
        }    
        
        
        
        //Part 4: Test the Efcp Connector  
        try
        {     
            System.out.println("\n\n\n*** UdpConnectorTest: Section 4  ***");
            
            EfcpConnector efcpConn1 = new EfcpConnector( 
                    new ConnectionShaper(
                        new UdpConnector(1188),
                        100, // 600ms min delay
                        20, // up to 300ms additional random delay (jitter).
                        1000 // 33% packet loss
                    ),
                    new EfcpPolicyInfo()
                    );
            efcpConn1.SetPeerAddress(InetAddress.getLocalHost(), 1189);
            
            EfcpConnector efcpConn2 = new EfcpConnector( 
                    new ConnectionShaper(
                        new UdpConnector(1189),
                        100, // 600ms min delay
                        20, // up to 300ms additional random delay (jitter).
                        1000 // 33% packet loss
                    ),
                    new EfcpPolicyInfo()
                    );
            efcpConn2.SetPeerAddress(InetAddress.getLocalHost(), 1188);
            
            for (int ii = 0; ii<80; ii++)
            {
                System.out.print("Test4 Process Send "+ ii +".\n");
                efcpConn1.Send("Test4_Packet_"+ii+".");
            }
            
            int packetsReceived = 0;
            
            System.out.print("Test 4 Process Receive 1: Shouldn't get anything.\n");
            List<byte[]> dataPacketsReceived = efcpConn2.Receive(600);
            for(byte[] data : dataPacketsReceived)
            {
                System.out.print("Test4 Process Receive 1: " + new String(data) + "\n");
                ++packetsReceived;
            }
            
            /// First receive may not have been able to pick up all packets (not yet arrived)
            try {Thread.sleep(1000);} catch(InterruptedException ex) {}
            int receivesTried = 1;
            while(packetsReceived<79)
            {
                System.out.print("Test 4 Process Receive "+ ++receivesTried +": Should get only in-order packets.\n");
                dataPacketsReceived = efcpConn2.Receive(1000);
                for(byte[] data : dataPacketsReceived)
                {
                    System.out.print("Test4 Process Receive "+ receivesTried +": " + new String(data) + "\n");
                    ++packetsReceived;
                }
            }
            
            // Try some more. 
            for (int ii = 80; ii<160; ii++)
            {
                System.out.print("Test4 Process Send "+ ii +".\n");
                efcpConn1.Send("Test4_Packet_"+ii);
            }
            while(packetsReceived<159)
            {
                System.out.print("Test 4 Process Receive "+ ++receivesTried +": Should get only in-order packets.\n");
                dataPacketsReceived = efcpConn2.Receive(1000);
                for(byte[] data : dataPacketsReceived)
                {
                    System.out.print("->Test 4 Process Receive " + receivesTried 
                            + ", packet " + packetsReceived 
                            + " contains " + new String(data) + "\n");
                    ++packetsReceived;
                }
            }
            System.out.print("SUCCESS. Got all 29 packets!!! \n");
            // Let these threads remain running - make sure there's no unfinished
            // cleanup or unresolved state that might cause continued activity.
            //efcpConn1.StopReceiveThread();
            //efcpConn2.StopReceiveThread();
        }
        catch(Exception e)
        {
            System.out.println("Test 4 Error:" + e.getMessage());
        }   

        System.out.print("\n\n****TEST DONE**** \n\n");
    }
 
}
