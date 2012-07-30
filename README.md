# AUSyncer
AUSyncer is library designed to simplyfy creation of syncable project

# Run an example

run this command:

    git clone <repository-url> AndroidAUSyncer
    
in eclipse:
 * File -> New -> Android Project from Existing Code
 * select AndroidAUSyncer directory
 * Finish
 * build and run ExampleAUSyncer

in ant:

   	cd ExampleAUSyncer
   	ant debug
   	ant installd

Example is based on methods from API level >= 11, but you can easly switch to API >= 7 thanks ActionBarSherlock library and switching imports

# Embeding in your project

run this command:

   git submodule add <repository-url> AndroidAUSyncer

in eclipse:
 * File -> New -> Android Project from Existing Code
 * select AndroidAUSyncer directory
 * Finish
 * Your project -> properties -> Android
 * Library -> Add..
 * select AUSyncer and OK

in ant:

 * XXX should be sequent number or 1 if first
 * add to your project project.properties file something like: "android.library.reference.XXX=../AndroidAUSyncer/AUSyncer"
 * run those commands:
 
		ant debug
		ant installd
		
# License

    Copyright [2012] [Jacek Marchwicki <jacek.marchwicki@gmail.com>]
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    	http://www.apache.org/licenses/LICENSE-2.0
        
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
