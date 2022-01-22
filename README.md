## TODO
* Move Clear button to top menu
* Page title on score entry page
* Edit rider name/class
* Display progress notification for both import and export
* Score formatter that can be shared between list and entry
* How to use DI?
* App icon
* Publish to app store
* ~score entry fragment scrolls to the top with each punch~
* ~30 sections~
* ~Close soft keyboard after saving rider info~
* ~Crash on back button when selecting file to download~
* ~Normalize CSV input to ignore extra spaces and quotes~
* ~more cleans is better place~
* ~Import rider list from CSV (Name, Class)~
* ~Use Spinner to select class~
* ~Fix build warnings~
* ~First rider in a class isn't marked as not finished~
* ~Sort riders who didn't finish by class/name instead of anything else~
* ~Mark riders who didn't finish yet~
* ~Sort entries on leaderboard by completed laps (desc), points (asc), name (asc)~
* ~Refactor leaderboard to aggregate results in the DB
  SELECT rs.name, rs.class, COUNT(ss.points) as sections_ridden,  SUM(ss.points) as points,
  SUM(CASE ss.points  WHEN 0 THEN 1 ELSE 0 END) AS cleans
  FROM rider_score AS rs
  LEFT JOIN section_score AS ss ON ss.riderId = rs.id AND ss.points >= 0
  GROUP BY rs.id
  ORDER BY class, sections_ridden DESC, points ASC, cleans DESC~
* ~Export event results to Excel~
* ~Group entries on leaderboard by class~
* ~Move score updating and merge logic out of ScoreCardViewModel. Not sure where it should be though~
* ~Add SectionScoresRepository and move load or create scores logic in it~
* ~Update score calculation to ignore not scored sections~
* ~Reset database between integration tests~
* ~Integration test for creating new rider record~
* ~Create new rider with empty score card~
* ~Reset score card: delete all entries in DB~
* ~Create new score card for rider - 10 sections with -1 points committed to the DB~

## Future ideas
* Configurable classes
* Split sections in to laps and enter one lap at a time

