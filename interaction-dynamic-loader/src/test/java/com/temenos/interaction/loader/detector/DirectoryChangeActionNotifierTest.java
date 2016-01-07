package com.temenos.interaction.loader.detector;

/*
 * #%L
 * interaction-dynamic-loader
 * %%
 * Copyright (C) 2012 - 2015 Temenos Holdings N.V.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.temenos.interaction.loader.detector.stub.DirectoryEventInterestedActionTestStub;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring-test-contexts/directory-loader-test-context.xml" })
public class DirectoryChangeActionNotifierTest {

    @Autowired
    DirectoryEventInterestedActionTestStub action;
    
    @Autowired
    DirectoryChangeActionNotifier notifier;
    
    @Test
    public void test() throws IOException, InterruptedException {
       Assert.assertNotNull(action);
       Assert.assertNotNull(notifier);
       Assert.assertTrue(!notifier.getListeners().isEmpty());
       
       File tempDir = createTempDirectory();
       notifier.setResources(Collections.singletonList(tempDir));
       
       Thread.currentThread().sleep(3000);
       
       File tempFile = new File(tempDir,"test.file");
       tempFile.createNewFile();
       Thread.currentThread().sleep(3000);
       Assert.assertTrue(action.getCallCount() > 0);
       System.out.println(action.getLastEvent().getResource());
       FileUtils.forceDelete(tempDir);
    }
    
    public static File createTempDirectory()
    throws IOException
    {
        final File temp;
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        temp = new File(tempDir, "test"+System.nanoTime());

        if(!(temp.mkdirs()))
        {
            throw new IOException("Could not create temporary test dir");
        }

        return (temp);
    }

}
