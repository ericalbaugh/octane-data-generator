<h1>HPE ALM Octane Data Generator</h1>

This tool generates demo data in an instance of <a href="https://www.youtube.com/watch?v=-ZzBEls_I6g">HPE ALM Octane</a> running in <a href ="https://saas.hpe.com/">HPE SaaS</a>.

<h2>Pre-requisities</h2>
To run the tool, you need to have <a href="https://java.com/en/download">Java 7 SE Runtime Edition</a> or above installed.
You need to also have an account in an octane instance running in <a href ="https://saas.hpe.com/">HPE SaaS</a>.

<h2>Instructions</h2>
To run the tool, execute:

<code>generate.bat --SpaceId=XXXX –WorkspaceId=XXXX your_hpe_saas_name your_hpe_saas_password</code>

<h2>Demo Data Content</h2>
The tool generates the following entity types in the provided instance:
<ul>
  <li>Release</li>
  <li>Teams</li>
  <li>Product Areas</li>
  <li>Epics</li>
  <li>Features</li>
  <li>Stories</li>
  <li>Defects</li>
  <li>Tasks</li>
</ul>

<h2>Customize Data Content</h2>
The tool iterates over sheets and rows in an <a href="https://github.com/pe-pan/octane-data-generator/raw/master/src/main/resources/data.xlsx">Excel file</a> that is built-in to the tool itself.

You have the option to customize this file content and provide the new file as the very first parameter when starting the tool.

<h2>Multiple runs</h2>
When running, the tool writes a job log with all the created entities; when running the tool multiple times, it first removes the previously generated entities (iterating over the lines in the job log). 
