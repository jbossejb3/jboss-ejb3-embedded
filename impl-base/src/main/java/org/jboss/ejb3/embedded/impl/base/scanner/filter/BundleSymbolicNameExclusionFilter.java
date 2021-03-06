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

package org.jboss.ejb3.embedded.impl.base.scanner.filter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.ejb3.embedded.spi.scanner.filter.ExclusionFilter;
import org.jboss.logging.Logger;
import org.jboss.vfs.VirtualFile;

/**
 * {@link ExclusionFilter} implementation which 
 * will block OSGi bundles with the header "Bundle-SymbolicName"
 * if the value matches one in a configurable set.
 * 
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class BundleSymbolicNameExclusionFilter implements ExclusionFilter
{

   //-------------------------------------------------------------------------------------||
   // Class Members ----------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Logger
    */
   private static final Logger log = Logger.getLogger(BundleSymbolicNameExclusionFilter.class);

   /**
    * Key of the bundle symbolic name header
    */
   private static final String HEADER_BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";

   /**
    * Location of the manifest file under the root
    */
   private static final String NAME_MANIFEST = "META-INF/MANIFEST.MF";

   //-------------------------------------------------------------------------------------||
   // Instance Members -------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Patterns to exclude if present in value of the bundle symbolic name header
    */
   private final Set<String> exclusionValues;

   //-------------------------------------------------------------------------------------||
   // Constructor ------------------------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * Creates a new instance configured to the specified exclusion values
    * @param exclusionValues Patterns to exclude if present in the bundle symbolic name header
    * @throws IllegalArgumentException If no exclusions are specified
    */
   public BundleSymbolicNameExclusionFilter(final String... exclusionValues) throws IllegalArgumentException
   {
      // Precondition check
      if (exclusionValues == null || exclusionValues.length == 0)
      {
         throw new IllegalArgumentException("one or more exclusion values must be specified");
      }

      // Defensive copy on set and make immutable
      final Set<String> excludeSet = new HashSet<String>();
      for (final String exclusionValue : exclusionValues)
      {
         excludeSet.add(exclusionValue);
      }
      this.exclusionValues = Collections.unmodifiableSet(excludeSet);

   }

   //-------------------------------------------------------------------------------------||
   // Required Implementations -----------------------------------------------------------||
   //-------------------------------------------------------------------------------------||

   /**
    * {@inheritDoc}
    * @see org.jboss.ejb3.embedded.spi.scanner.filter.ExclusionFilter#exclude(org.jboss.vfs.VirtualFile)
    */
   @Override
   public boolean exclude(final VirtualFile file)
   {
      // Precondition checks
      if (file == null)
      {
         throw new IllegalArgumentException("file must be specified");
      }

      // If this exists, first of all
      if (!file.exists())
      {
         return false;
      }

      // Get the Manifest
      final VirtualFile manifest = file.getChild(NAME_MANIFEST);
      if (!manifest.exists())
      {
         return false;
      }

      // Inspect the manifest contents
      final LineNumberReader reader;
      try
      {
         reader = new LineNumberReader(new InputStreamReader(manifest.openStream()));
         String line = null;
         // Read each line
         while ((line = reader.readLine()) != null)
         {
            // If this is the bundle symbolic name header
            final String header = HEADER_BUNDLE_SYMBOLIC_NAME;
            if (line.contains(header))
            {
               // Check if it also contains a matching value
               for (final String exclusionValue : this.exclusionValues)
               {
                  if (line.contains(exclusionValue))
                  {
                     if (log.isTraceEnabled())
                     {
                        log.tracef("Configured exclusion value \"" + exclusionValue
                              + "\" encountered in manifest header \"" + header + "\"; skipping " + file);
                     }
                     // Skip
                     return true;
                  }
               }
            }
         }
      }
      catch (final IOException ioe)
      {
         throw new RuntimeException("Could not read contents of " + file, ioe);
      }

      // No conditions met
      return false;
   }

}
