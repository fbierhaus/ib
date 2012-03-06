/**
 * 
 */
package org.jboss.netty.example.echo;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

/**
 * @author fred
 *
 */
public class ClientThread implements Runnable {

	private ChannelBuffer imapCommand;
	private ChannelFuture channelFuture;
	
	public ClientThread(ChannelBuffer imapCommand, ChannelFuture channelFuture){
		this.imapCommand = imapCommand;
		this.channelFuture = channelFuture;
	}
	
	protected void sendMessage(){
		Channel channel = channelFuture.getChannel();
		System.out.println("Writing: " + imapCommand);
		channel.write(imapCommand);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		sendMessage();
	}

}
