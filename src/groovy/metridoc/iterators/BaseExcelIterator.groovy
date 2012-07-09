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


/**
 * Created by IntelliJ IDEA.
 * User: tbarker
 * Date: 9/16/11
 * Time: 9:13 AM
 * To change this template use File | Settings | File Templates.
 */
abstract class BaseExcelIterator extends FileIteratorCreator {

    /**
     * if a sheet name is specified, it trumps any sheet index if specified
     */
    String sheetName
    /**
     * default is 0, will be ignored if a {@link BaseExcelIterator#sheetName} is specified
     */
    int sheetIndex = 0

    int getRowNum() {
        return getLine() - 1 //excel rows are zero based
    }

    protected static int convertColumnToNumber(String column) {
        def m = (column =~ /(\D+)\d*/)  //strip out row numbers
        m.find()
        def justColumn = m.group(1)

        String lowercase = justColumn.toLowerCase()

        def columnNumber = 0
        int a = 'a'

        for (int i = 0; i < lowercase.size(); i++) {
            int power = lowercase.size() - i - 1
            int charRelativeTo_a = lowercase.charAt(i) - a + 1
            def charInBase26 = (charRelativeTo_a) * (26 ** power)
            columnNumber += charInBase26
        }

        return columnNumber
    }
}