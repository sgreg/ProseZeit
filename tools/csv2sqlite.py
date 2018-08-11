#
#   ProseZeit - Literary Clock Widget for Android
#   CSV to SQLite conversion script
#
#   Copyright (C) 2018 Sven Gregori <sven@craplab.fi>
#   MIT License
#
#   Takes tjaap's CSV quote collection file and transforms it into
#   a SQLite database containing
#       - the time converted as minute of the day
#       - the quote itself, bolding the matching time parts
#       - the book name
#       - the author name
#   to be used as asset shipped with the ProseZeit app.
#
import sys
import csv
import sqlite3

if len(sys.argv) < 3:
    print('usage: %s <csv infile> <sqlite3 outfile>' % (sys.argv[0], ))
    sys.exit(1)

infile = sys.argv[1]
outfile = sys.argv[2]

# open / create the database
conn = sqlite3.connect(outfile)
c = conn.cursor()

# create the database table
c.execute('CREATE TABLE quotes (_id INTEGER PRIMARY KEY, minute INTEGER, text TEXT, author TEXT, book TEXT)')
conn.commit()

with open(infile) as csv_file:
    csv_reader = csv.reader(csv_file, delimiter='|')
    line_count = 1;
    for row in csv_reader:
        # calculate minute of day from time
        minute = int(row[0][:2]) * 60 + int(row[0][3:])
        # enclose the matching pattern with some HTML bold tags
        text = row[2].replace(row[1], '<b>' + row[1] + '</b>', 1)
        # make all the quotation types (', ", "", """) the same
        text = text.replace('"""', '""').replace('""', '"').replace('"', "'")
        book = row[3].replace('"""', '""').replace('""', '"').replace('"', "'")
        author = row[4].replace('"""', '""').replace('""', '"').replace('"', "'")

        # write entry to database
        try:
            c.execute('INSERT INTO quotes (minute, text, author, book) values(%d, "%s", "%s", "%s")' % (minute, text, author, book))
        except sqlite3.OperationalError as e:
            print('error in line %d: %s' % (line_count, e))
            break

        line_count += 1;

    print('%d lines processed' % (line_count,))

# clean up
conn.commit()
conn.close()

