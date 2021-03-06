/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
  *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.ejb3.embedded.impl.base;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;

import junit.framework.Assert;

import org.jboss.bootstrap.api.descriptor.BootstrapDescriptor;
import org.jboss.bootstrap.api.lifecycle.LifecycleState;
import org.jboss.bootstrap.api.mc.server.MCServer;
import org.jboss.bootstrap.api.mc.server.MCServerFactory;
import org.jboss.ejb3.embedded.api.JBossEJBContainer;
import org.jboss.reloaded.api.ReloadedDescriptors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Ensures that the {@link JBossEJBContainerProviderBase} 
 * implementation is working as contracted by {@link JBossEJBContainer}
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class JBossEJBContainerBaseUnitTest
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * The underlying MC server
    */
   private static final MCServer server = MCServerFactory.createServer();

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * {@link JBossEJBContainer} instance under test
    */
   private JBossEJBContainerBase ejbContainer;

   //-------------------------------------------------------------------------------------||
   // Lifecycle --------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Configures and starts the underlying MC server
    */
   @BeforeClass
   public static void startMc() throws Exception
   {

      // Define the descriptors to use to start VDF
      final List<BootstrapDescriptor> descriptors = server.getConfiguration().getBootstrapDescriptors();
      descriptors.add(ReloadedDescriptors.getClassLoadingDescriptor());
      descriptors.add(ReloadedDescriptors.getVdfDescriptor());

      // Start
      server.start();

   }

   /**
    * Creates the EJB Container under test
    */
   @Before
   public void createEJBContainer()
   {
      ejbContainer = new TestJBossEJBContainer(new HashMap<Object, Object>(), server, new String[]
      {});
   }

   /**
    * Cleans up and shuts down MC
    * @throws Exception
    */
   @AfterClass
   public static void stopMc() throws Exception
   {
      // Shutdown
      if (server != null && server.getState().equals(LifecycleState.STARTED))
      {
         server.stop();
      }
   }

   //-------------------------------------------------------------------------------------||
   // Required Implementations -----------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Ensures we honor {@link JBossEJBContainer#getContext()}
    * @throws Exception
    */
   @Test
   public void jndiContextFromEjbContainer() throws Exception
   {

      // Install the naming server into MC;
      final File namingServerDeploymentFile = new File(Thread.currentThread().getContextClassLoader().getResource(
            "naming-server-jboss-beans.xml").toURI());
      ejbContainer.deploy(namingServerDeploymentFile.toURI().toURL());

      // Get a Context via the EJB Container
      final Context context = ejbContainer.getContext();
      Assert.assertNotNull("Got null JNDI context from EJB Container", context);

      // Bind into JNDI
      final Object objectToBind = new Pojo();
      final String bindName = "bindName";
      context.bind(bindName, objectToBind);

      // Ensure we've bound correctly by looking up from JNDI as a round trip
      Assert.assertSame("Object bound was not as expected", objectToBind, context.lookup(bindName));

      // Undeploy the JNDI server
      ejbContainer.undeploy(namingServerDeploymentFile.toURI().toURL());

   }

   //-------------------------------------------------------------------------------------||
   // Functional Methods -----------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   //-------------------------------------------------------------------------------------||
   // Internal Helper Methods ------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Test concrete extension of the 
    */
   private static class TestJBossEJBContainer extends JBossEJBContainerBase
   {

      TestJBossEJBContainer(final Map<?, ?> properties, final MCServer server, final String[] modules)
      {
         super(properties, server, modules);
      }

      /**
       * {@inheritDoc}
       * @see javax.ejb.embeddable.EJBContainer#close()
       */
      @Override
      public void close()
      {
         //NOOP
      }

   }

   /**
    * A simple test Pojo we'll check for installation
    * 
    * 
    * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
    * @version $Revision: $
    */
   public static class Pojo implements Serializable
   {

      /**
       * serialVersionUID
       */
      private static final long serialVersionUID = 1L;

   }
}
