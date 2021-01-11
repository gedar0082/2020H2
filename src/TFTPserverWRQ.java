import java.net.*;
import java.io.*;




class TFTPserverWRQ extends Thread {

	protected DatagramSocket sock;
	protected InetAddress host;
	protected int port;
	protected FileOutputStream outFile;
	protected TFTPpacket req;
	protected int timeoutLimit = 5;
	protected File saveFile;
	protected String fileName;

	public TFTPserverWRQ(TFTPwrite request) throws TftpException {
		try {
			req = request;
			sock = new DatagramSocket();
			sock.setSoTimeout(1000);

			host = request.getAddress();
			port = request.getPort();
			fileName = request.fileName();

			saveFile = new File("../"+fileName);

			if (!saveFile.exists()) {
				outFile = new FileOutputStream(saveFile);
				TFTPack a = new TFTPack(0);
				a.send(host, port, sock);
				this.start();
			} else
				throw new TftpException("access violation, file exists");

		} catch (Exception e) {
			TFTPerror ePak = new TFTPerror(1, e.getMessage()); // error code 1
			try {
				ePak.send(host, port, sock);
			} catch (Exception f) {
			}

			System.out.println("Client start failed:" + e.getMessage());
		}
	}

	public void run() {

		if (req instanceof TFTPwrite) {
			try {
				for (int blkNum = 1, bytesOut = 512; bytesOut == 512; blkNum++) {
					while (timeoutLimit != 0) {
						try {
							TFTPpacket inPak = TFTPpacket.receive(sock); 
							//check packet type
							if (inPak instanceof TFTPerror) {
								TFTPerror p = (TFTPerror) inPak;
								throw new TftpException(p.message());
							} else if (inPak instanceof TFTPdata) {
								TFTPdata p = (TFTPdata) inPak;

								if (p.blockNumber() != blkNum) {

									throw new SocketTimeoutException();
								}

								bytesOut = p.write(outFile);
								TFTPack a = new TFTPack(blkNum);
								a.send(host, port, sock);

								break;
							}
						} catch (SocketTimeoutException t2) {
							System.out.println("Time out, resend ack");
							TFTPack a = new TFTPack(blkNum - 1);
							a.send(host, port, sock);
							timeoutLimit--;
						}
					}
					if(timeoutLimit==0){throw new Exception("Connection failed");}
				}
				System.out.println("Transfer completed.(Client " +host +")" );
				System.out.println("Filename: "+fileName);
				
			} catch (Exception e) {
				TFTPerror ePak = new TFTPerror(1, e.getMessage());
				try {
					ePak.send(host, port, sock);
				} catch (Exception f) {
				}

				System.out.println("Client failed:  " + e.getMessage());
				saveFile.delete();
			}
		}
	}
}