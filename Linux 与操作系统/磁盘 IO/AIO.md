# Asynchronous File IO on Linux

# APIs

## aio_read()

aio_read() is the function where we tell the system what file we want to read, the offset to begin the read, how many bytes to read, and where to put the bytes that are read. All of this information goes into a aiocb structure, which looks like this:

- aio_fildes - file descriptor of the open file you want to read from
- aio_offset - Offset where you want to begin reading
  aio_nbytes - Number of bytes to read
  aio_buf - Pointer to the buffer where the bytes read will be put

There's plenty of other members inside this structure, but they aren't important to us right now. But it's still a good idea to zero out the structure before you use it. memset() would work fine for this.

## aio_error()

aio_error() checks the current state of the IO request. Using this function you can find out of the request was successful or not. All you have to do is give it the address of the same aiocb structure that you gave aio_read(). The function returns 0 if the request completed successfully, EINPROGRESS if it's still working, or some other error code if an error occured.

By the way, you may be wondering if this means you'll have to create a different aiocb for each request. Well, you do. It should be obvious that if you muck around with an aiocb while a request is currently being fulfilled, bad things can happen. It should also be obvious that the buffer you give the aiocb will need to remain in existance the whole time the request is being fulfilled. So don't give it a pointer to a stack array and then jump out of the function. Bad things.

## aio_return()

aio_return() checks the result of an IO request once you find out the request has been finished. If the request succeeded, this function returns the number of bytes read. If it failed then the function returns -1.

# 案例：代码实现

```c
#include <sys/types.h>
#include <aio.h>
#include <fcntl.h>
#include <errno.h>
#include <iostream>

using namespace std;

const int SIZE_TO_READ = 100;

int main()
{
	// open the file
	int file = open("blah.txt", O_RDONLY, 0);

	if (file == -1)
	{
		cout << "Unable to open file!" << endl;
		return 1;
	}

	// create the buffer
	char* buffer = new char[SIZE_TO_READ];

	// create the control block structure
	aiocb cb;

	memset(&cb, 0, sizeof(aiocb));
	cb.aio_nbytes = SIZE_TO_READ;
	cb.aio_fildes = file;
	cb.aio_offset = 0;
	cb.aio_buf = buffer;

	// read!
	if (aio_read(&cb) == -1)
	{
		cout << "Unable to create request!" << endl;
		close(file);
	}

	cout << "Request enqueued!" << endl;

	// wait until the request has finished
	while(aio_error(&cb) == EINPROGRESS)
	{
		cout << "Working..." << endl;
	}

	// success?
	int numBytes = aio_return(&cb);

	if (numBytes != -1)
		cout << "Success!" << endl;
	else
		cout << "Error!" << endl;

	// now clean up
	delete[] buffer;
	close(file);

	return 0;
}
```
