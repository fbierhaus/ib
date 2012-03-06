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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
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
    private final int numRequests;
    private final ChannelBuffer messageBuffer;
    
    public EchoClient(String host, int port, String message, int numClients, int numRequests) {
        this.host = host;
        this.port = port;
        this.numClients = numClients;
        this.numRequests = numRequests;
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
                return Channels.pipeline(new EchoClientHandler(messageBuffer));
            }
        });

        ArrayBlockingQueue <Runnable> workQueue = new ArrayBlockingQueue<Runnable>(numRequests);
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(numClients, numClients + 3, 10, TimeUnit.SECONDS, workQueue);
        
        ChannelGroup allChannels = new DefaultChannelGroup();
        
        // Start the connection attempts.
        ChannelFuture[] channelFutures = fireEmUp(numClients, bootstrap);
        for(int i = 0; i < numClients; i++){
        	allChannels.add(channelFutures[i].getChannel());
        	threadPoolExecutor.execute(new ClientThread(messageBuffer, channelFutures[i]));
        }

        allChannels.close().awaitUninterruptibly();
        
        // Shut down thread pools to exit.
        threadPoolExecutor.shutdown();
        bootstrap.releaseExternalResources();
    }
    
    protected ChannelFuture[] fireEmUp(int numClients, ClientBootstrap bootstrap){
    	ChannelFuture[] channelFutures = new ChannelFuture[numClients];
    	for(int i = 0; i < numClients; i++){
    		channelFutures[i] = bootstrap.connect(new InetSocketAddress(host, port));
    		// Do we want to wait here?
    		channelFutures[i].awaitUninterruptibly();
    		if(!channelFutures[i].isSuccess()){
    			System.err.println("Channel " + i + " failed: " + channelFutures[i].getCause());
    			// Die here, if we cant connect all the channels we want
    			// This is a load test, after all
    			System.exit(-1);
    		}else {
    			System.out.println("Connected: " + i);
    		}
    	}
    	return channelFutures;
    }

    public static void main(String[] args) throws Exception {
    	
    	// create Options object
    	Options options = new Options();

    	options.addOption("h", true, "host");
    	options.addOption("p", true, "port");
    	options.addOption("n", true, "number of requests");
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
        final int numRequests = Integer.parseInt(cmd.getOptionValue("n"));

        new EchoClient(host, port, imap_command, numClients, numRequests).run();
    }
}

