package net.praqma.stash.plugins.tracey.components.impl;

import net.praqma.stash.plugins.tracey.components.api.BrokerService;
import net.praqma.stash.plugins.tracey.components.api.BrokerConfigurationService;
import net.praqma.stash.plugins.tracey.exceptions.BrokerServiceException;
import net.praqma.tracey.broker.api.TraceyBroker;
import net.praqma.tracey.broker.api.TraceyIOError;
import net.praqma.tracey.broker.impl.rabbitmq.RabbitMQConnection;
import net.praqma.tracey.broker.impl.rabbitmq.RabbitMQRoutingInfo;
import net.praqma.tracey.broker.impl.rabbitmq.TraceyRabbitMQBrokerImpl;
import net.praqma.tracey.broker.impl.rabbitmq.TraceyRabbitMQSenderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMQBrokerServiceImpl implements BrokerService {
    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQBrokerServiceImpl.class);
    private final TraceyBroker broker;

    public RabbitMQBrokerServiceImpl(final BrokerConfigurationService brokerConfigurationService) {
        // Read configuration when it is implemented
        this.broker = new TraceyRabbitMQBrokerImpl();
        final RabbitMQConnection connection = new RabbitMQConnection(((RabbitMQBrokerConfigurationServiceImpl) brokerConfigurationService).getHost(),
                ((RabbitMQBrokerConfigurationServiceImpl) brokerConfigurationService).getPort(),
                ((RabbitMQBrokerConfigurationServiceImpl) brokerConfigurationService).getUsername(),
                ((RabbitMQBrokerConfigurationServiceImpl) brokerConfigurationService).getPassword(),
                true);
        broker.setSender(new TraceyRabbitMQSenderImpl(connection));
    }

    @Override
    public void send(String message, RabbitMQRoutingInfo destination) throws BrokerServiceException{
        LOG.debug("Ready to send the following message to desination " + destination + "\n" + message);
        try {
            broker.send(message, destination);
        } catch (TraceyIOError error) {
            throw new BrokerServiceException("Failed to send RabbitMQ messages", error);
        }
    }
}