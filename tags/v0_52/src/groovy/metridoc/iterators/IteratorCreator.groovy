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
 * Date: 9/7/11
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IteratorCreator<T, U> {
    public static final String ITERATOR_CREATOR_PARAMETERS = "MetridocIteratorCreatorParameters"

    Iterator<U> create(T file, Map headers)
    Iterator<U> create(T body)
}