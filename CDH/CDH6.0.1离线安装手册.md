**CDH6.0.1离线安装手册**

文章编写目的
------------

在大多数的项目当中，可能网络安装组件是很常见的一件事。但由于这次的项目是为银行服务，所以在线安装组件就不可取了。采用了离线安装的方式，结合某某银行的需求，在这里记一次离线安装CDH的方式。CDH安装的步骤分为四部分内容：

1.  基础环境准备和配置；

2.  安装包及配置YUM源；

3.  Cloudera Manger安装；

4.  CDH集群安装；

需要注意的是CDH6安装必须包括如下几点：

-   外部数据库支持

>   MYSQL5.6+

>   MariaDB5.5+

>   PostgreSQL8.4+

>   Oracle 12c+

-   JDK

>   JDK1.8，将不会支持JDK1.7

-   操作系统支持

>   RHEL6.8+

>   Ubutun16+

本次的测试环境如下：

1.  CM和CDH版本为6.0.1

2.  Centos7.2

3.  JDK1.8.0\_111

4.  MYSQL5.6

5.  Root用户安装

基础环境准备和配置
------------------

### 2.1.基础环境准备

#### .2.1.1.下载Cloudera 管理器需要的RPM包


     地址： https://archive.cloudera.com/cm6/6.0.1/redhat7/yum/RPMS/x86\_64/ 


#### 2.1.2.下载Parcel包

     地址： https://archive.cloudera.com/cdh6/6.0.1/parcels/ 



#### 2.1.3.文件工具准备

    CM包名称：
    cloudera-manager-agent-6.0.1-610811.el7.x86\_64.rpm 
    cloudera-manager-daemons-6.0.1-610811.el7.x86\_64.rpm 
    cloudera-manager-server-6.0.1-610811.el7.x86\_64.rpm
    cloudera-manager-server-db-2-6.0.1-610811.el7.x86\_64.rpm
    oracle-j2sdk1.8-1.8.0+update141-1.x86\_64.rpm
    repodata

    parcels包名称：
    CDH-6.0.1-1.cdh6.0.1.p0.590678-el7.parcel
    CDH-6.0.1-1.cdh6.0.1.p0.590678-sles12.parcel.sha256
    manifest.json

    Mysql包名称：
    mysql-5.6.32-linux-glibc2.5-x86\_64.tar.gz
    mysql-connector-java-5.1.39-bin.jar

    Yum源：
       cloudera-manager.repo 

#### 2.1.4.节点规划

| 主机名 | IP地址         | 操作系统  | 资源               |
|--------|----------------|-----------|--------------------|
| cdh1   | 192.168.44.141 | Centos7.2 | 内存：9G CPU:4core |
| cdh2   | 192.168.44.131 | Centos7.2 | 内存：9G CPU:4core |
| cdh3   | 192.168.44.129 | Centos7.2 | 内存：7G CPU:6core |

备注：cat /etc/redhat-release

### 2.2.配置

#### 2.2.1.hostname及hosts配置

**Root用户下，修改主机名（hostname）**
  vim /etc/hostname  
   添加主机名称：
    cdh1
  其他节点相同添加相应的主机名



**修改ip与主机名对应关系hosts文件（所有节点）**
vim /etc/hosts 

       192.168.44.141 cdh1 
       192.168.44.131 cdh2 
       192.168.44.129 cdh3 



#### 2.2.2.关闭防火墙及SELinux

注意：需要在所有的节点上执行，安装完毕后可以根据需要设置防火墙策略，保证集群安全。

**关闭防火墙**

    systemctl stop firewalld.service 
    systemctl disable firewalld.service 



**关闭SELINUX**
vim /etc/selinux/config 

     SELINUX=disabled 



#### 2.2.3.集群时钟同步

说明：集群中所有主机必须保持时间同步，一些组件将依赖于主机的时间，如果时间相差较大会引起各种问题。

具体思路如下：

主节点作为ntp服务器与外界对时中心同步时间，随后对所有从节点提供时间同步服务。

如果无法连接至外部网络，可以设置好主节点时间，随后对所有从节点提供时间同步服务。

因为大部分的情况都无法连接至外部网络，所以本文只提供本地服务的配置方法。

**方法一：**

**主节点配置（ntp服务器配置）**

\---同步本地时间(没有的行手动添加)

    vim /etc/ntp.conf
       driftfile /var/lib/ntp/drift
       restrict 127.0.0.1
       restrict -6 ::1
       restrict default nomodifynotrap
       server127.127.1.0
       fudge 127.127.1.0 stratum 8
       includefile /etc/ntp/crypto/pw
       keys /etc/ntp/keys

其他用\#屏蔽掉，配置文件完成，保存退出，启动服务，执行如下命令：

    service ntpd start

检查是否成功，用ntpstat命令查看同步状态，出现以下状态代表启动成功

如果出现异常请等待几分钟，一般等待5-10分钟才会正常。

**从节点配置（ntp客户端配置）**
    
     vim /etc/ntp.conf 
       driftfile /var/lib/ntp/drift 
       restrict 127.0.0.1 
       restrict -6 ::1 
       server 192.168.44.141 //主节点ip 
       restrict default kodnomodifynotrapnopeernoquery 
       restrict -6 default kodnomodifynotrapnopeernoquery 
       includefile /etc/ntp/crypto/pw keys /etc/ntp/keys 

其他用\#屏蔽掉，配置文件完成，保存退出。

说明：

（1）server 这里是主节点的主机名或者ip （注意是server，不是service）

（2）启动ntp客户端服务前，请先使用ntpdate手动同步一下时间（不建议手动同步，可忽略此步骤）：ntpdate
ntp服务器ip地址

（3）这里可能出现同步失败的情况，一般是ntp服务器还没有正常启动，一般需要等待5-10分钟才可以正常同步。

\*\*\*启动ntp客户端服务：service ntpd start

\*\*\*做开机启动chkconfig ntpd on

\*\*\*查看ntp服务是否同步成功用 ntpstat
，输出结果最后一行的第一个数据是\*表明同步成功，同步需要一点时间。

#### 2.2.4 修改Linux内核参数（所有节点）

为避免安装过程中出现的异常问题，首先调整Linux内核参数。

**设置swappiness值**

1.  查看当前系统swappiness值：

>   cat /proc/sys/vm/swappiness

  ![](https://i.imgur.com/rRESfTW.png)

显示Cloudera建议将 /proc/sys/vm/swappiness设置为 0

1.  修改swappiness值：

        sysctl vm.swappiness=0 
        cat /proc/sys/vm/swappiness 
        vim /etc/sysctl.conf 
        将以下内容粘贴到文件末尾： 
        vm.swappiness = 10 



注意：如果不修改此项的值，Cloudera Manager
报告您的主机交换运行状况不佳的警告。该值默认为30.

**3.说明下swappiness：**

Linux系统的swap分区并不是等所有的物理内存都消耗完毕之后，才去使用swap分区的空间，什么时候使用是由swappiness参数值控制。

swappiness=0的时候，表示最大限度使用物理内存，然后才使用swap空间，

swappiness=100的时候，表示积极的使用swap分区，并且把内存上的数据及时的搬运到swap空间中。

现在服务器的内存越来越高，我们可以把参数值设置的低一些，让操作系统尽可能的使用物理内存，降低系统对swap的使用，从而提高系统的性能。（也可以设置为10
）

**关闭transparent\_hugepage**

首先看透明大页是否启用，[always] madvise never表示启用，always madvise
[never]表示已禁用；

查看：

\# cat /sys/kernel/mm/transparent\_hugepage/defrag

\# cat /sys/kernel/mm/transparent\_hugepage/enabled

![](https://i.imgur.com/47AYRns.png)

修改：

 vim /etc/rc.d/rc.local 
  在文件内容后面加如下内容 

     if test -f /sys/kernel/mm/transparent\_hugepage/enabled; then 
       echo never \> /sys/kernel/mm/transparent\_hugepage/enabled 
    fi
     if test -f /sys/kernel/mm/transparent\_hugepage/defrag; then 
       echo never \> /sys/kernel/mm/transparent\_hugepage/defrag 
     fi 

chmod +x /etc/rc.d/rc.local 



修改完且重启系统后查看

**修改句柄数**

查看默认的句柄数是1024，数值太小

\#ulimit -n

![](https://i.imgur.com/sOD1sdO.png)

修改限制：

 vim /etc/security/limits.conf 
添加： 

    soft nofile 65536 
    hard nofile 65536 


重启后再查看

#### 安装JDK

在CentOS中，自带OpenJdk，不过运行CDH5需要使用Oracle的Jdk，需要Java 7+的支持。

**卸载自带的openjdk**

查询java相关的包

命令：rpm -qa \| grep java

卸载相应的包名

命令：rpm -e --nodeps包名

**安装jdk1.8**

rpm -ivh jdk-8u111-linux-x64.rpm

vim \~/.bashrc 
    
     #java for env
     export JAVA\_HOME=/usr/java/jdk1.8.0\_111 
     export JAVA\_BIN=/usr/java/jdk1.8.0\_111 
     export PATH=\$PATH:\$JAVA\_HOME/bin 
     export CLASSPATH=.:\$JAVA\_HOME/lib/dt.jar:\$JAVA\_HOME/lib/tools.jar 
     export JAVA\_HOME JAVA\_BIN PATH CLASSPATH
     
source \~/.bashrc 

安装好后用 java –version 来查看是否安装成功

#### 配置SSH免密钥登陆

>   在所有上执行如下这条命令：

>   ssh-keygen -t rsa

一路回车，将生成无密码的密钥对。

在所有节点将公钥添加到认证文件中

 `cat \~/.ssh/id\_rsa.pub \>\> \~/.ssh/authorized\_keys`

 把从节点生成的authorized\_key文件内容复制到主节点authorized\_key中 

并设置authorized\_keys的访问权限

    chmod 600 \~/.ssh/authorized\_keys 



scp文件到所有从节点:

     scp -r /root/.ssh/authorized\_keys user\@ip: /root/.ssh/ 


输入命令：

ssh 从节点主机名

可以直接登录到从节点，不需要密码

#### 安装mysql服务

1.  **检查mysql及相关RPM包是否安装，如果有，则移除。**


     rpm -qa|grep mysql 
     rpm -e --nodeps mysql-libs 


2.  **解压并安装**

   方法：使用ftp工具，将本地的包上传至linux系统的某一位置；然后在那个位置上进行解压。

本文使用的安装包mysql-5.6.32-linux-glibc2.5-x86\_64.tar.gz，解压后安装

   解压命令：tar –xvf mysql-5.6.32-linux-glibc2.5-x86\_64.tar.gz -C /usr/local

  重命名解压后的目录：mv mysql-5.6.32-linux-glibc2.5-x86\_64 mysql

设置权限：

    useradd mysql chown -R mysql:mysql mysql echo '123456'\|passwd --stdin 'mysql' 


进入mysql/scripts 目录：

     ./mysql\_install\_db --basedir=/usr/local/mysql --datadir=/usr/local/mysql/data --user=mysql


1.  **修改配置文件**


       [root\@h003 support-files]\# cp my-default.cnf /etc/my.cnf
       [root\@h003 support-files]\# vim /etc/my.cnf 
         [mysqld] 
            datadir=/usr/local/mysql/data 
            socket =/usr/local/mysql/data/mysql.sock 
            character\_set\_server=utf8 
            lower\_case\_table\_names=1 \#不区分大小写 
         [client] 
            socket =/usr/local/mysql/data/mysql.sock 
         [mysql.server] 
            user=mysql 
            basedir=/usr/local/mysql 
      [root\@h003 support-files]\# cp mysql.server /etc/init.d/mysql 
      [root\@h003 init.d]\# vim /etc/init.d/mysql 
        添加：
         basedir=/usr/local/mysql 
         datadir=/usr/local/mysql/data 
    
1.  **设置环境变量**

        [root\@h003 support-files]\# vim \~/.bashrc 
         #mysql for env 
         export MYSQL\_HOME=/usr/local/mysql 
         export PATH=\$MYSQL\_HOME/bin:\$PATH 

2. **设置开机启动及初始化数据库**
   设置开机启动：

       chkconfig --add mysql 
       chkconfig mysql on 


3.** 启动mysql服务：** 

    service mysql start

4.**设置登陆密码：**

     [root\@h003 bin]\# ./mysqladmin -u root password 123456 
     [root\@Master bin]\# mysql -u root -p123456 


5.**远程登陆授权**

  需要先登陆mysql,：

     grant all privileges on \*.\* to 'root'\@'%' identified by '123456' with grant option; 
     grant all privileges on \*.\* to 'root'\@'localhost' identified by '123456' with grant option; 
     grant all privileges on \*.\* to 'root'\@'cdh1' identified by '123456' with grant option; 
     flush privileges; 


解释：grant all privileges on \*.\* to 'root'\@'%' identified by
'123456';加后面的话会给的权限过高，此权限是可以创建新用户的。\@‘%’
表示对所有非本地主机授权，不包括localhost。如果使用%授权出问题，应该是用户不对，在mysql里面select
\* from mysql.user ，看看有几个localhost。

注意：这时需要关闭防火墙或者配置防火墙规则，否则无法远程连接至MySQL数据库

安装包及配置YUM源
-----------------

### 3.1.上传的目录安装包

把所需要的yum源需要的安装包用FTP上传。

### 3.2.创建YUM源

在/etc/yum.repos.d下配置

       [root\@cdh1 yum.repos.d]\# vim cloudera-manager.repo 
         [cloudera-manager] 
            name=Cloudera Manager 6.0.1 
            baseurl=http://192.168.44.141:8889/cm/ 
            gpgcheck=0 
            enabled = 1 

 备注：IP是存放安装包的服务器IP地址 Yum源命令： yum clean all yum makecache yum update                                                                             

### 3.5.启动YUM源

     [root\@cdh1 yum.repos.d]\# cd /usr/local/cdh
     [root\@cdh1 cdh]\# nohup python -m SimpleHTTPServer 8889 & 

 备注：此服务启动需要在/usr/local/cdh目录下启动（其他目录不可行） netstat -pantu\|grep 8889              

四.CM安装
---------

### 4.1.cloudera-manager 安装

    yum install cloudera-manager-daemons cloudera-manager-server 

### 4.2.mysql驱动包

    mv mysql-connector-java-5.1.39-bin.jar mysql-connector-java-5.1.39.jar 
    cp mysql-connector-java-5.1.39.jar /usr/share/java cd /usr/share/java 
    ln -s mysql-connector-java-5.1.39.jar mysql-connector-java.jar 


### 4.3创建数据库

进入mysql, mysql -uroot -p123456

创建以下数据库

    #hive 
      create database hive DEFAULT CHARSET utf8 COLLATE utf8\_general\_ci; 
    #oozie 
      create database oozie DEFAULT CHARSET utf8 COLLATE utf8\_general\_ci; 
    #cm 
      create database cm DEFAULT CHARSET utf8 COLLATE utf8\_general\_ci; 
    #hue 
      create database hue DEFAULT CHARSET utf8 COLLATE utf8\_general\_ci; 
    #monitor 
      create database monitor DEFAULT CHARSET utf8 COLLATE utf8\_general\_ci; 

 备注：hive,oozie,hue没有组件，无需安装                                                                                                                                                                                                                                                                                                                                                        

### 4.4.初始化数据库

    /opt/cloudera/cm/schema/scm\_prepare\_database.sh \<DB\_TYPE\> \<DATABASE\> –h \<MYSQL\_HOST\> -u\<USERNAME\> -p\<PASSWORD\> --scm-host localhost scm scm scm   

 举例：/opt/cloudera/cm/schema/scm\_prepare\_database.sh mysql cm –hlocalhost -uroot -pxxxx --scm-host localhost scm scm scm 
 备注：rpm包和在线安装可能放置的路径不同，需要find / -name scm\_prepare\_database.sh查找一下 

### 4.5.启动Cloudera管理服务器

    service cloudera-scm-server start                  

备注：启动后就可以访问cloudera管理页面，监听端口7180 

### 4.6.检查端口是否被监听

     netstat -pantu|grep 7180 



通过*http://192.168.44.141:7180/cmf/login*访问CM

![](media/a3ce040e535ac0b55b6247c441aa0d7e.png)

### 4.7.CDH集群安装

#### 4.7.1. CDH集群安装向导

1. admin/admin 登陆到CM

![](https://i.imgur.com/13gnra0.png)

2.同意license协议,点击继续

![](https://i.imgur.com/lBNmlbm.png)

3.选择免费,点击继续

![](https://i.imgur.com/UkhKY7L.png)

4.点击继续

![](https://i.imgur.com/Fws0XKw.png)

5.选择“继续”，输入主机IP或者主机名称，点击搜索到主机后点击继续
   ![](https://i.imgur.com/MFW7ReU.png)
   ![](https://i.imgur.com/16AirFD.png)

6.点击“继续”

   ![](https://i.imgur.com/ursDNDo.png)

7.使用Parcel选择，点击“更多选项”，点击“-”，删除其他所有地址，输入<http://192.168.44.141:8889/parcels/>，点击“保存更改“
    ![](https://i.imgur.com/iWW6MXa.png)

8.选择定义存储库，输入CM的http地址
![](https://i.imgur.com/bioDxUe.png)



9.点击”继续“，进入下一步安装JDK(JDK已安装可忽略)

   ![](https://i.imgur.com/QL8M7H0.png)

10.点击“继续”，进入下一步配置ssh账号密码
![](https://i.imgur.com/AT218XX.png)

11.点击“继续”，进入下一步，安装Cloudera Manager相关到各个节点

 等待agent安装完毕后，自动跳转到下一步开始分发parcel

12.点击“继续”，进入下一步安装cdh到各个节点
![](https://i.imgur.com/Vtiv3VA.png)


13.点击“继续”，进入下一步主机检查，确保所有检查项均通过。机器因为有多个Java版本有一些警告，此步忽略
![](https://i.imgur.com/CPGMLrE.png)


点击完成进入服务安装向导

#### 4.7.2.集群设置安装向导

1.选择需要安装的服务

![](https://i.imgur.com/h3uDQtb.png)

2.点击“继续”，进入集群角色分配

![](https://i.imgur.com/oGGEWFb.png)

![](https://i.imgur.com/kGNe7As.png)
3.点击“继续”，进入下一步，测试数据库连接

![](https://i.imgur.com/GuH4Gzr.png)

4.测试成功，点击“继续”，进入目录设置，此处使用默认默认目录，根据实际情况进行目录修改
![](https://i.imgur.com/aYmHha7.png)

5.点击“继续”，进入各个服务启动

![](https://i.imgur.com/tW0vdSV.png)

6.安装成功后进入home管理界面

![](https://i.imgur.com/qgyqa2P.png)

#### **4.7.3.组件版本检查**
![](https://i.imgur.com/pmrmbTR.png)

  可以看到Hadoop3.0，Flume1.8，HBase2.0，Hive2.1，Spark2.2，Hue3.9，Impala3.0，Kafka1.0.0，Kudu1.6，Oozie5.0，Pig0.17，Senty2.0，Solr7.0，Sqoop1.4.7，Zookeeper3.4.5等


