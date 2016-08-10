import getopt, sys
import random
import string

def main():
	try:
		opts, args = getopt.getopt(sys.argv[1:], "hc:r:", ["help", "output="])
	except getopt.GetoptError as err:
		# print help information and exit:
		print(err) # will print something like "option -a not recognized"
		usage()
		sys.exit(2)
	output = None
	verbose = False
	for o, a in opts:
		if o == "-c":
			ncolumns = int(a)
		elif o == "-r":
			nrows = int(a)
		elif o in ("-h", "--help"):
			usage()
			sys.exit()
		else:
			assert False, "unhandled option"

	types = {
		"string"          : 0.05,
		"varchar(64)"     : 0.25,
		"char(4)"         : 0.4,
		"boolean"         : 0.45,
		"decimal(16, 10)" : 0.55,
		"float"           : 0.7,
		"int"             : 1
	}

	keys = types.keys()
	keys = sorted(keys, key=lambda type: types[type])
	columns = []
	random.seed(1)
	basename = str(ncolumns) + "_data";
	while ncolumns > 0:
		rand = random.random()
		for k in keys:
			if rand <= types[k]:
				columns.append(k)
				break
		ncolumns = ncolumns - 1

	# Generate the DDL
	fd = open("%s.ddl" % basename, "w")
	fd.write("drop table if exists test_txt;\n");
	fd.write("drop table if exists test_orc;\n");
	fd.write("create table test_txt(\n")
	i = 1
	colstrings = []
	for column in columns:
		colstring = "\tc%d %s" % (i, column)
		colstrings.append(colstring)
		i = i + 1
	fd.write(",\n".join(colstrings))
	fd.write("\n);\n")
	fd.write("load data local inpath '%s.csv' overwrite into table test_txt;\n" % basename);
	fd.write("create table test_orc like test_txt;\n");
	fd.write("alter table test_orc set fileformat orc;\n");
	fd.write("insert into table test_orc select * from test_txt;\n");
	fd.close()

	# Generate the data.
	fd = open("%s.csv" % basename, "w")
	for i in range(0, nrows):
		coldata = []
		for column in columns:
			coldata.append(getRandomStuff(column))
		fd.write(",".join(coldata))
		fd.write("\n")

def getRandomStuff(column):
	if column[0:3] == "str":
		return randomText(16)
	if column[0:3] == "var":
		return randomText(24)
	if column[0:3] == "cha":
		return randomText(4)
	if column[0:3] == "boo":
		if (random.random() > 0.5):
			return "T"
		else:
			return "F"
	if column[0:3] == "dec":
		value = str(random.randint(1, 9999)) + "." + str(random.randint(1, 999))
		return value
	if column[0:3] == "flo":
		return str(random.random() * 500)[0:7]
	if column[0:3] == "int":
		return str(random.randint(1, 9999))

def randomText(length):
	#char_set = string.ascii_uppercase + string.ascii_lowercase + " " + string.digits
	char_set = string.ascii_uppercase + " ";
	char_set = "ABCDEFGHIJKLMN ";
	return ''.join(random.sample(char_set * 3, length))

if __name__ == "__main__":
	main()
