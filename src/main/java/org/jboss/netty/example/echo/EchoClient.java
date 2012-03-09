/*
 * Copyright 2011 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.example.echo;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

/**
 * Sends one messageBuffer when a connection is open and echoes back any received
 * data to the server.  Simply put, the echo client initiates the ping-pong
 * traffic between the echo client and server by sending the first messageBuffer to
 * the server.
 */
public class EchoClient {

    private final String host;
    private final int port;
    private final int numClients;
    private final ChannelBuffer messageBuffer;
    
    public EchoClient(String host, int port, String message, int numClients) {
        this.host = host;
        this.port = port;
        this.numClients = numClients;
        this.messageBuffer = ChannelBuffers.copiedBuffer(message + "\r\n", Charset.forName("US-ASCII"));
    }

    public void run() {
        // Configure the client.
        ClientBootstrap bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new EchoClientHandler());
            }
        });

       
        
        // Start the connection attempts.
        ChannelGroup allChannels = fireEmUp(numClients, bootstrap);

        // Iterate and write
        Iterator<Channel> iter = allChannels.iterator();
        int n = 0;
        while (iter.hasNext()) {
			Channel channel = (Channel) iter.next();
			channel.write(messageBuffer);
			if (n%100 == 0) {
				try {
					Thread.sleep(50);
					System.out.println(n + " messages written");
				} catch (InterruptedException e) {
				}
			}
			n++;
		}
        
        // let's try sleeping so the client doesn't close the channel before 
        // the server has written response
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
        
        allChannels.close().awaitUninterruptibly();
        
        // Shut down thread pools to exit.
        bootstrap.releaseExternalResources();
    }
    
    protected ChannelGroup fireEmUp(int numClients, ClientBootstrap bootstrap){
        ChannelGroup allChannels = new DefaultChannelGroup();
        ChannelFuture channelFuture;
    	for(int i = 0; i < numClients; i++){
    		channelFuture = bootstrap.connect(new InetSocketAddress(host, port));
    		// Do we want to wait here?
    		channelFuture.awaitUninterruptibly();
    		if(!channelFuture.isSuccess()){
    			System.err.println("Channel " + i + " failed: " + channelFuture.getCause());
    			// Die here, if we can't connect all the channels we want
    			// This is a load test, after all
    			System.exit(-1);
    		}else {
            	allChannels.add(channelFuture.getChannel());
            	if (i%100 == 0) {
					System.out.println(i + " channels connected");
				}
    		}
    		// sleep for a little to allow server to catch up
    		if (i%100 == 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
    	}
    	return allChannels;
    }

    public static void main(String[] args) throws Exception {
    	
    	// create Options object
    	Options options = new Options();

    	options.addOption("h", true, "host");
    	options.addOption("p", true, "port");
    	options.addOption("c", true, "number of concurrent connections");
    	
    	CommandLineParser parser = new PosixParser();
    	CommandLine cmd = parser.parse( options, args);
    	
    	String[] remainder = cmd.getArgs();
    	String imap_command = null;
    	if((remainder != null) && (remainder.length > 0)){
        	imap_command = remainder[remainder.length -1];
    	}
    	

        // Parse options.
        final String host = cmd.getOptionValue("h");
        final int port = Integer.parseInt(cmd.getOptionValue("p"));
        final int numClients = Integer.parseInt(cmd.getOptionValue("c"));
        new EchoClient(host, port, imap_command, numClients).run();

    }
}

