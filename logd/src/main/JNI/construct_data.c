#include <string.h>
#include "construct_data.h"
#include "cJSON.h"
#include "stdlib.h"
#include "json_util.h"

Construct_Data_cLogan *
construct_json_data_clogan(const char *log, int flag, long long local_time, const char *thread_name,
                           long long thread_id, int is_main) {
    Construct_Data_cLogan *construct_data = NULL;
	construct_data = (Construct_Data_cLogan *) malloc(sizeof(Construct_Data_cLogan));
	if (NULL != construct_data) {
		memset(construct_data, 0, sizeof(Construct_Data_cLogan));
		size_t str_len = strlen(log);
		unsigned char *temp_data = (unsigned char *) malloc(str_len+1);
		if (NULL != temp_data) {
			unsigned char *temp_point = temp_data;
			memset(temp_point, 0, str_len);
			memcpy(temp_point, log, str_len);
			temp_point += str_len;
			char return_data[] = {'\n'};
			memcpy(temp_point, return_data, 1); //添加\n字符
			construct_data->data = (char *) temp_data; //赋值
			construct_data->data_len = (int) str_len+1;
		} else {
			free(construct_data); //创建数据
			construct_data = NULL;
		}
	}
    return construct_data;
}

void construct_data_delete_clogan(Construct_Data_cLogan *item) {
    if (NULL != item) {
        if (NULL != item->data) {
            free(item->data);
        }
        free(item);
    }
}
