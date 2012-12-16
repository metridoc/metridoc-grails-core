/*
 * Copyright 2010 Trustees of the University of Pennsylvania Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package metridoc.core

import org.apache.commons.lang.ObjectUtils
import org.junit.Test
import org.apache.camel.component.file.GenericFile

import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.component.file.GenericFileFilter
import org.apache.commons.lang.SystemUtils

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 6/25/12
 * Time: 9:57 AM
 */
class CamelRoutingTest {

    def job = new CamelRoutingJob()

    @Test
    void testBasicRouteCall() {
        job.executeTarget("mockBasic")
    }

    @Test
    void testFullRoute() {
        job.executeTarget("fullRoute")
    }

    @Test
    void hitJobLater() {
        job.executeTarget("hitMeLater")
    }

}

class CamelRoutingJob extends MetridocJob {

    def fileFilter = [
            accept: {GenericFile file ->
                def response = file.fileName.startsWith("file")
                return response
            }
    ] as GenericFileFilter

    @Override
    def doExecute() {
        target(mockBasic: "a very simple camel routing test") {
            MockEndpoint mock = camelJobContext.getEndpoint("mock:endBasic")
            mock.reset()
            mock.expectedMessageCount(1)
            producerJobTemplate.requestBody("mock:endBasic", ObjectUtils.NULL)
            mock.assertIsSatisfied()
        }

        target(fullRoute: "a more complicated route") {
            MockEndpoint mock = camelJobContext.getEndpoint("mock:endFull")
            mock.reset()
            mock.expectedMessageCount(1)
            createTempDirectoryAndFiles()

            runRoute {
                from("file://${tmpDirectory.path}?noop=true&initialDelay=0&filter=#fileFilter").threads(4).aggregateBody(4, 2000).to("mock:endFull")
            }

            mock.assertIsSatisfied()

            deleteTempDirectoryAndFiles()
        }

        target(hitMeLater: "run a test to see if we can hit a job later on") {
            runRoute {
                from("direct:hitMeLater").to("mock:gettingDataFromLaterHit")
            }

            MockEndpoint mock = camelJobContext.getEndpoint("mock:gettingDataFromLaterHit")
            mock.expectedMessageCount = 1
            producerJobTemplate.requestBody("direct:hitMeLater", ObjectUtils.NULL)
            mock.assertIsSatisfied()
        }
    }

    def getTmpDirectory() {
        def home = SystemUtils.USER_HOME
        new File("${home}/.metridoctmp")
    }

    def deleteTempDirectoryAndFiles() {
        deleteFiles()

        tmpDirectory.delete()
    }

    def deleteFiles() {
        tmpDirectory.listFiles().each {
            it.delete()
        }
    }

    def createTempDirectoryAndFiles() {
        def home = SystemUtils.USER_HOME
        def tempDirectory = new File("${home}/.metridoctmp")
        tempDirectory.mkdir()
        deleteFiles()

        File.createTempFile("unused", "metridocTest", tempDirectory)
        File.createTempFile("file1", "metridocTest", tempDirectory)
        File.createTempFile("file2", "metridocTest", tempDirectory)
        File.createTempFile("file3", "metridocTest", tempDirectory)
        File.createTempFile("file4", "metridocTest", tempDirectory)
    }
}
