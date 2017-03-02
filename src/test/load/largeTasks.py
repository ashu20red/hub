# locust.py
import os
import random
import string


class LargeTasks:
    hubTasks = None
    first = True

    def __init__(self, user, hubTasks):
        self.user = user
        self.hubTasks = hubTasks

    def large_file_name(self, number, direction):
        return '/mnt/large' + str(number) + '.' + direction

    def write(self):
        if self.first:
            self.create_large(self.hubTasks)
            self.first = False
        large_file_name = self.large_file_name(self.hubTasks.number, 'out')
        large_file = open(large_file_name, 'rb')
        expected_size = os.stat(large_file_name).st_size
        with self.hubTasks.client.post(self.hubTasks.get_channel_url(),
                                       data=large_file,
                                       headers={"content-type": "application/octet-stream"},
                                       catch_response=True,
                                       name="post_payload") as postResponse:
            if postResponse.status_code != 201:
                postResponse.failure("Got wrong response on post: " + str(postResponse.status_code)
                                     + self.hubTasks.get_channel_url())
        uri = postResponse.json()['_links']['self']['href']

        with self.client.get(uri, catch_response=True, name="get_payload") as getResponse:
            if getResponse.status_code != 200:
                getResponse.failure("Got wrong response on get: " + str(getResponse.status_code) + " " + uri)
                return
        input = self.large_file_name(self.hubTasks.number, 'in')
        with open(input, 'wb') as fd:
            for chunk in getResponse.iter_content(chunk_size=1024):
                fd.write(chunk)
        get_size = os.stat(input).st_size
        if get_size != expected_size:
            getResponse.failure("Got wrong size on get: " + str(get_size) + " " + uri)

    def create_large(self, tasks):
        large_file_name = self.large_file_name(self.hubTasks.number, 'out')
        if os.path.isfile(large_file_name):
            print "using existing file " + large_file_name + " bytes=" + str(os.stat(large_file_name).st_size)
            return
        if tasks.number == 1:
            target = open(large_file_name, 'w')
            print "writing file " + large_file_name
            target.truncate(0)
            chars = string.ascii_uppercase + string.digits
            size = 50 * 1024
            for x in range(0, 10 * 1024):
                target.write(''.join(random.choice(chars) for i in range(size)))
                target.flush()

            print "closing " + large_file_name
            target.close()
        elif tasks.number == 2:
            os.system("cat /mnt/large1.out /mnt/large1.out > /mnt/large2.out")
        elif tasks.number == 3:
            os.system("cat /mnt/large2.out /mnt/large2.out > /mnt/large3.out")
