
AWS Summit：现代化的架构是使用AWS的服务、Lambda的功能，把它们连接在一起。没有中间层、没有应用层，也没有数据库层，它是一系列web服务连接在一起，由功能连接在一起，无需服务器，而安全、可靠性、规模、性能、成本管理这些事项由AWS做好。在AWS的观点里，我认为Serverless不是指FaaS，而是指上面讲的这个现代化架构。"Everyone wants just to focus on business logic."

UC Berkley 认为 Serverless 是 One Step Forward, Two Steps Back。关于One Step Forward的观点为带来的主要是云资源的弹性的使用。关于Two Steps Back的观点为：Function的情况下，每个Function是独立的，Function之间的交互是通过持久或临时的存储、事件驱动来完成，导致了完成交互的时间比以前慢了很多很多。并且通常分布式系统会依赖很多的类似leader election协议、数据一致性、事务机制等，而这些在目前的FaaS类型的平台里是很难去实现的。

软件发展到今天，多数企业的业务系统开始越来越复杂化，开发一个业务系统需要掌握和关注的知识点越来越多，并且系统中出现了越来越多的非业务的基础技术系统，例如分布式cache等等，在这种情况下，研发的门槛在上升，效率在下降，而Serverless思想我觉得很重要的就是用于解决研发门槛和效率的问题，让业务系统研发能更专注的关注在业务逻辑上（和AWS说的现代化架构的观点一致），而不仅仅是充分发挥云资源的弹性，按量付费降低成本那么简单。

核心的点就在于把业务系统开发时需要用到的各种基础技术产品都隐藏起来，并将要用到的一些基础技术进行归纳抽象，例如存储、服务交互。

Managed Services里面当然包括AWS S3，DynamoDB，SNS，SQS等。这些Managed Service不仅在Serverless里面可以用，在server program里面也在用，但是很显然，这些丰富的Managed Service极大地促进了Serverless ——可以让开发者在体感上完全去除server的概念。 例如，虽然Serverless里面也可以用RDS，但因为RDS后面你还需要自己管理Server，因此总体的感觉依然是Server based。