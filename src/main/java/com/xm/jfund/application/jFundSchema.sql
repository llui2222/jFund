drop table if exists jFundSymbolsOfInterest;

create table jFundSymbolsOfInterest(
    SymId int not null,

    foreign key (SymId) references SymbolDetails.SymId
);

insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURUSD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'GBPUSD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDJPY';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDCHF';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'AUDUSD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURAUD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURCAD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURCHF';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURDKK';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURGBP';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURHKD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURHUF';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURJPY';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURNOK';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURPLN';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURSEK';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURSGD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURZAR';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'EURNZD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'NZDUSD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDCAD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDDKK';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDHKD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDHUF';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDMXN';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDNOK';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDPLN';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDSEK';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDSGD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDTRY';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDZAR';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'XAGUSD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'XAUUSD';
insert into jFundSymbolsOfInterest select SymId from SymbolDetails where SymName = 'USDCNH';
