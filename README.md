# LogTask
creates database Analysis in working directory and rewrites data from log file to Event table

usage: LogTask file [-d] [-r]
  file - log file name
  -r - report database content
  -d - delate data from database

// to run with argument from gradle e.g.
// gradle run -PappArgs="['x:\\test\\event.log', '-d', '-r']"
