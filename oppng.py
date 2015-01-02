import os

def walk_dir(dir,topdown=True):
	for root, dirs, files in os.walk(dir, topdown):
		for name in files:
			full_path = os.path.join(root,name)
			if full_path[-4:]==".png" :
				os.system("optipng "+full_path)

if __name__ =='__main__':
	walk_dir('./app/src')