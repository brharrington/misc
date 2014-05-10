#!/usr/bin/perl

use strict;
use warnings;

use Data::Dumper;
use Getopt::Long;
use Pod::Usage;

sub getJavaPid {
  my $name = shift;
  for my $line (`jps`) {
    if ($line =~ /^(\d+) $name$/) {
      return $1;
    }
  }
  die "could not find pid for '$name'";
}

sub getStackTrace {
  my $pid = shift;
  my $filter = shift;

  my $threads = {};

  my $thread = undef;
  my $stacktrace = "";
  my $skip = 0;
  my @lines = `jstack $pid`;
  for my $line (@lines) {
    if ($line =~ /^"([^"]+)"/) {
      $thread = $1;
      $stacktrace = "";
    } elsif ($line =~ /^\s*$/) {
      $threads->{$thread} = $stacktrace
        if ($thread && $thread =~ /$filter/ && $stacktrace !~ /^\s*$/);
      $thread = undef;
    } else {
      $stacktrace .= "$line";
    }
  }

  return $threads;
}

sub main {
  my $help = undef;
  my $interval = 5;
  my $samples = 10;
  my $n = 5;
  my $filter = ".*";
  my $jpsName = "Bootstrap";
  GetOptions(
    "help|h"       => \$help,
    "interval|i=i" => \$interval,
    "samples|s=i"  => \$samples,
    "n=i"          => \$n,
    "filter|f=s"   => \$filter,
    "jps=s"        => \$jpsName) or pod2usage(1);
  pod2usage(1) if $help;

  my $pid = getJavaPid($jpsName);
  print STDERR "java pid: $pid\n";

  my $traces = [];
  for (my $i = 1; $i <= $samples; $i++) {
    print STDERR "collecting stacktrace $i of $samples\n";
    my $trace = getStackTrace($pid, $filter);
    push(@$traces, $trace);
    sleep($interval);
  }

  my $counts = {};
  for my $trace (@$traces) {
    for my $stack (values %$trace) {
      if (defined $counts->{$stack}) {
        $counts->{$stack}++;
      } else {
        $counts->{$stack} = 1;
      }
    }
  }

  my @mostCommon = sort { $counts->{$b} <=> $counts->{$a} } keys %$counts;
  my $i = 0;
  for my $stack (@mostCommon) {
    my $count = $counts->{$stack};
    printf("%10d  ========================================================\n", $count);
    print $stack;
    print "\n\n";
    $i++;
    last unless $i < $n;
  }
}

main;

=head1 NAME

jstack_histo.pl - creates histogram for java stack traces, sort | uniq -c | sort -nrk1 for jstack
                  output

=head1 SYNOPSIS

jstack_histo.pl --jps Bootstrap -n 10 -i 5

=head1 OPTIONS

=over 4

=item B<--help, -h>

Print a brief help message and exits.

=item B<--interval, -i>

How many seconds to wait between samples. Default is 5.

=item B<--samples, -s>

Number of samples to collect. Default is 10.

=item B<-n>

How many traces to output. Default is 1.

=item B<--filter, -f>

Regex used to restrict the set of theads checked. Default is ".*".

=item B<--jps>

Name of the java process under jps to collect data for. Default is "Bootstrap".

=back

=cut
