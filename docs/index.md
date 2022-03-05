## Summary

This is Android application to help count score for motorcycle trials events.
Its main goal is to help event organizers to count scores and share them quickly at the event location.
It does not require cell reception.
It's only available for Android 8+.
It's in development stage.
It's free and released under GPLv3 license.

<img src="img/leaderboard.png" width="300" alt="Leaderboard screen" />

<img src="img/score-entry.png" width="300" alt="Score entry screen" />

## Usage scenario
1. Configure event: classes, number of laps and sections (incomplete)  
2. Import rider list
3. Manually enter or edit rider information if needed
4. Record scores as riders turn in their cards
5. Share scores
6. Export scores to generate final score sheets

### Configure event
This feature is not implemented and currently hardcoded for the following classes:
* Champ
* Expert
* Advanced
* Intermediate
* Novice

Events are configure to display 30 sections in single loop.

### Import rider list
Assuming you have rider list exported from your registration system. 
Create Excel spreadsheet with just 2 columns: rider name and rider class.
It's best to keep rider class matching classes you configured in the app.
Otherwise you'll have to edit each rider in the app and set correct class.

| Tamara Murazik | Advanced |
| --- | --- |
| Damian Brakus | Intermediate |
| Uriel Mills | Expert |
| Sabrina McGlynn | Advanced | 
| Shannon Ritchie | Novice |
| Baylee Cruickshank | Intermediate |

Note, do not include header as application will try to import it as a rider.
Save the spreadsheet as CSV file.
Most spreadsheet application use comma as delimiter, this should work just fine.
If you want to check raw exported file, it should look like this.

```csv
Tamara Murazik,Advanced
Damian Brakus,Intermediate
Uriel Mills,Expert
Sabrina McGlynn,Advanced
Shannon Ritchie,Novice
Baylee Cruickshank,Intermediate
```

Copy file onto device and import the list using application menu from the main screen.

<img src="img/import-riders.png" width="300" alt="Import menu" />

### Manually enter or edit rider info

You can add new rider from Leaderboard (home) screen.
You can edit rider info from score entry screen. 
Simply tap on rider name and you should see "pencil" icon.

### Entering scores
Leaderboard is sorted by using the following order:

* finished riders
  * points
  * cleans
* not finisher riders (marked by asterisk)
  * first name

Tapping on rider name opens their score entry screen.
Results are saved as you "punch" the score card.
When finished simply use "back" button to return to previous screen.
There is no way to "undo" score entry.
You can clear entire score for the rider using "Clear rider score" menu option in the top right corner.

### Sharing scores
The only way to share scores at the moment is by giving the score entry device to a rider.
They may alter scores as there is currently no way to lock entries.

### Export results
This application doesn't attempt to generate nice score sheets. 
Instead it lets you export raw results into Excel and friends.
You can then generate final score sheets as you probably do right now.

Export is available from the Leaderboard screen.
It generate CSV file that you can import into your spreadsheets application.
Sample output, once imported, look like this.

| Name | Class | Points | Cleans | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | S9 | S10 | S11 | S12 | S13 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Armani Bergstrom | Expert | 20 | 13 | 0 | 1 | 1 | 0 | 1 | 0 | 1 | 0 | 0 | 0 | 1 | 1 | 2 |  
| Cecilia Collier | Expert | 5   | 26 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 
| Dorothy Rogahn | Expert | 76   |  0 | 1 | 2 | 1 | 1 | 3 | 2 | 5 | 3 | 2 | 3 | 2 | 1 | 2 |

## How to install
As of March 2022 this application is in internal testing is on its first field test. 
I plan to make it publicly available once it allows you to configure classes.
If you want to give it a try before that please get in touch with me on [facebook](https://www.facebook.com/vitali.yakavenka/).

## Roadmap
See issues on GitHub for future ideas and their status 
