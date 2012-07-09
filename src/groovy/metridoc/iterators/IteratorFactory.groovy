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
package metridoc.iterators

import liquibase.util.file.FilenameUtils
import org.apache.camel.Body
import org.apache.camel.Header
import org.apache.camel.component.file.GenericFile

/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 12/6/11
 * Time: 10:08 AM
 */
class IteratorFactory implements IteratorCreator<GenericFile, List> {

    private FileIteratorCreator chooseIterator(GenericFile genericFile) {
        String extension = FilenameUtils.getExtension(genericFile.fileNameOnly);

        if ("gz" == extension) {
            def m = genericFile.fileNameOnly =~ /.*\.(\w+)\.gz$/
            m.matches()
            extension = m.group(1)
        }

        switch (extension) {
            case 'xls':
                return new XlsIterator()
            case 'xlsx':
                return new XlsxIterator()
            case 'csv':
                return new CsvIterator()
        }

        return new LineIterator();
    }

    Iterator<List> create(@Body GenericFile file, @Header(IteratorCreator.ITERATOR_CREATOR_PARAMETERS) Map headers) {
        def iteratorCreator = chooseIterator(file)
        iteratorCreator.create(file, headers)
    }
}