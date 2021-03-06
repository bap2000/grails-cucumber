/*
 * Copyright 2012-2013 Martin Hauner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugin.cucumber

import cucumber.runtime.PathWithLines
import cucumber.runtime.formatter.FormatterFactory


class RuntimeOptionsBuilder {
    def configObject

    RuntimeOptionsBuilder (ConfigObject configObject) {
        this.configObject = configObject
    }

    RuntimeOptions init (RuntimeOptions options, Map<String, Object> args) {
        addTags (options)
        addFormatter (options, args.format as String)
        addGluePaths (options)
        addFeaturePaths (options)
        addFilter (options, args.params as List<String> ?: [])
        options
    }

    static def addFilter (RuntimeOptions options, List<String> args) {
        if (args.size() < 2 || ! args.first().contains(":cucumber")) {
            return
        }

        def filters = []
        def featurePaths = []

        args.each { arg ->
            switch (arg) {
                case ~/\w*:cucumber/:
                    break
                case ~/~?@.+(:\d)?/:
                    filters << arg
                    break
                default:
                    PathWithLines pathWithLines = new PathWithLines (arg)
                    featurePaths << pathWithLines.path
                    filters.addAll (pathWithLines.lines)
            }
        }

        if (! filters.empty) {
            updateFilters (options.filters, filters)
        }

        if (! featurePaths.empty) {
            updateFeaturePaths (options.featurePaths, featurePaths)
        }
    }

    private static void updateFilters (List<Object> filters,List<Object> newFilters) {
        filters.clear ()
        filters.addAll (newFilters)
    }

    private static void updateFeaturePaths (List<String> featurePaths, List<String> newFeaturePaths) {
        String singleFeaturePath = featurePaths.first ()
        long numberOfFeaturePath = featurePaths.size ()

        featurePaths.clear ()

        newFeaturePaths.each {
            String path = it

            if (numberOfFeaturePath == 1 && ! path.startsWith (singleFeaturePath)) {
                path = [singleFeaturePath, path].join (File.separator)
            }

            featurePaths << path
        }
    }

    private void addGluePaths (RuntimeOptions options) {
        if (configObject.cucumber.glue) {
            options.glue.clear ()
            configObject.cucumber.glue.each { path ->
                options.glue << path.toString () // *NO* GString
            }
        } else {
            options.glue << configObject.cucumber.defaultGluePath
        }
    }

    private void addFeaturePaths (RuntimeOptions options) {
        if (configObject.cucumber.features) {
            options.featurePaths.clear ()
            configObject.cucumber.features.each { path ->
                options.featurePaths << path.toString () // *NO* GString
            }
        } else {
            options.featurePaths << configObject.cucumber.defaultFeaturePath
        }
    }

    private void addFormatter (RuntimeOptions options, String format) {
        // clear the 'default' cucumber formatter
        options.formatters.clear ()

        FormatterFactory factory = new FormatterFactory()
        if (format) {
            options.formatters << factory.create (format)
        }
        else {
            configObject.cucumber.formats.each {
                options.formatters << factory.create (it)
            }
        }
    }

    private void addTags (RuntimeOptions options) {
        configObject.cucumber.tags.each {
            options.filters.add (it)
        }
    }

}
