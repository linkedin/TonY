#
# Copyright 2021 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
# See LICENSE in the project root for license information.
#
import os
import time

tb_port = None
if 'TB_PORT' in os.environ:
    tb_port = os.environ['TB_PORT']

job_name = os.environ['JOB_NAME']

print('TB_PORT is ' + str(tb_port))
print('JOB_NAME is ' + job_name)

time.sleep(30)

if tb_port and job_name == 'chief':
    raise ValueError