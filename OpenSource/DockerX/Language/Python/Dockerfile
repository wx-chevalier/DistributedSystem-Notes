FROM continuumio/anaconda3
MAINTAINER wxyyxc1992 <384924552@qq.com>

# 设置环境变量
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8
ENV ENTRY=api.py

# 设置环境变量
ENV PATH /opt/conda/bin:$PATH

# 设置运行目录
RUN mkdir /opt/workspace
WORKDIR /opt/workspace

ADD ./run.sh /opt/workspace/run.sh
RUN chmod +x /opt/workspace/run.sh

# 设置应用入口点
ENTRYPOINT [ "/opt/workspace/run.sh" ]