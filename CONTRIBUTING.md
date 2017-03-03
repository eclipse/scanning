# Contributing to Eclipse Scanning

## Basic Ethos
We try to make the Eclipse Scanning project the heart of GDA9 at Diamond Light Source. Unlike previous versions of GDA we would like to make this system entirely open and very easy to reuse. To this end there are .product files in the repository to make running a dummy acquisition server and a dummy acquisition client very easy. There are also .target files to define the external repositories which provide the required libraries to run the project.

The dummy server starts a local activemq (which can be switched off from the arguments). This messaging system is the only route for client and server connections for this project. Unlike previous versions of GDA, there is no CORBA or RMI to configure. The ethos of the project is to be code agnostic. Therefore JSON has been chosen with text messages. The text messages may be sent and received in any language, for instance in python STOMP and in Java JMS but activeMQ supports a wide range of languages. No data is sent in the messaging system. The NeXus file format has been used to store data and link information is published using activemq.

## Tests!
If you would like to contribute, you should provide tests for everything that you add to the system. Do not be surprised if your reviewers ask for more tests to be created, in fact in many cases they should. The committers to the project are looking for tests which check for success but attempt to break your new feature. They should attempt to understand the limits that your code works within. Your tests should also run fast - we would like to keep running the build in no more than ten minutes. To help with speed, try not to create large files which travis nodes do not always like. The tests must be junit tests included in a Suite called 'Suite.java' in order for them to be run in travis automatically.

## Guidelines
(In random order.)

* There is no strict format for code, such as you must indent using a tab rather than four spaces or your imports should be in a specific order. Or a line limit other than the one Java suggests. If a reviewer asks for this you should refer to this guideline! 
* Avoid reformatting code just for 'readability' because it creates false diffs. Better to tolerate the original developer's formatting.
* The standard Java class naming (camel case with first upper case letter) and method naming (camel case with a first lower letter) should be followed. 
* Classes should not consist of many lines, for instance if your class grows to 1000's of lines, consider delegating parts of it to other classes. 
* Do make sure that classes which are intended to be used outside a package are public and those concerned with implementing the functionality locally are not! 
* Ensure that methods which are not part of the intended external API are private or protected. 
* Consider delegating rather than using inheritance, try to keep inheritance trees short. For instance Interface->Abstract Class->Concrete Class is the maximum.
* There is no rule that interfaces should start with the letter 'I'. Some programmers have chosen to follow this methodology but it is not a requirement of the project.
* Keep interfaces for OSGi services in no (where no means no:) dependency projects. It should be possible to get interfaces from OSGi without making dependencies. Examples of this are that the project often uses connector patterns to hide a specific implementation. For instance a connector to scanning devices (EPICSv4) or a JSON API would be created to isolated using services to ensure that the details of the underlying implementation do not leak out. This means that we can swap easily between EPICSv4 and 0MQ or JSON and XML for instance.
* The travis tests must run with junit not junit plugin
* Try not to forget the EPL license header (we will automatically add these when the project moves to eclipse)

## Route 1 - Become a Committer
* 1 Ask to be a member of the project, you can send a message to the project leader to do this.
* 2 When the project is in eclipse, vote will be required (through the eclipse foundation web pages). Currently a conversation with Matthew Webber and Matthew Gerring is required.
* 3 You then will be able to push your local changes to a branch on github
* 4 When ready create a pull request from your branch, the travis tests will be run automatically for your pull request
* 5 You may update your branch by pushing to it and the tests will be automatically rerun.
* 6 You should then invite other committers to review the change on github and respond to their comments.
* 7 Merge up to your branch the items to address their feedback, the tests will be automatically rerun.

## Reoute 2 - Non-Committer Pull Request
* Fork the repository on github and push your changes there.
* Create a pull request from your fork to Eclipse Scanning
* Follow steps 4-7 above from here.
* A committer will merge your request once the test passes and the review items are addressed.

# Code of Conduct (Based on HomeBrew CoC)
The community is made up of members from around the globe with a diverse set of skills, personalities, and experiences. It is through these differences that our community experiences great successes and continued growth. When you're working with members, we encourage you to follow these guidelines which help steer our interactions and strive to keep the project a positive, successful, and growing community.

A member is:

## Open
Open to collaboration, whether it's on GitHub, email or otherwise. We're receptive to constructive comment and criticism, as the experiences and skill sets of other members contribute to the whole of our efforts. We're accepting of all who wish to take part in our activities, fostering an environment where anyone can participate and everyone can make a difference.

## Considerate
Members of the community are considerate of their peers - other users and developers. We're thoughtful when addressing the efforts of others, keeping in mind that oftentimes their labor was completed simply for the good of the community. We're attentive in our communications, whether in person or online, and we're tactful when approaching differing views.

## Respectful
Members of the community are respectful. We're respectful of others, their positions, their skills, their commitments, and their efforts. We're respectful of the volunteer efforts that permeate the community. We're respectful of the processes set forth in the community, and we work within them. When we disagree, we are courteous in raising our issues.

Overall, we're good to each other. We contribute to this community not because we have to, but because we want to. If we remember that, these guidelines will come naturally.

# Diversity
The project welcomes and encourages participation by everyone. Our community is based on mutual respect, tolerance, and encouragement, and we are working to help each other live up to these principles. We want our community to be more diverse: whoever you are, and whatever your background, we welcome you.
