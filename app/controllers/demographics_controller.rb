class DemographicsController < ApplicationController
  def daily_summary
    @mode = YAML.load_file("#{Rails.root}/config/application.yml")['dde_mode'] rescue 'vh'
     day = params[:start_date].to_date
    new_nat_ids = NationalIdentifier.find(:all, :conditions => ["DATE(assigned_at) = ?", day])
    @today_count = new_nat_ids.length
    @today_gender_count = gender_counter(new_nat_ids.map{|x| x.person})
    @today_ages = age_categorizer(new_nat_ids.map{|x| x.person})
    @today_outcomes = outcome_sorter(Outcome.find(:all, :conditions => ["DATE(outcome_date) = ?", day]))
    @cumulative = cumulative_summarizer(day)
    @village = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["village"] rescue nil
    @gvh = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["gvh"] rescue nil
    @ta = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["ta"] rescue nil
    render :layout => 'report'
  end

  def gvh_reports
  end

  def gvh_statistics
    render :layout => 'report'
  end

  def gvh_outcomes_by_village
    date = params[:start_date].to_date
    @duration = date.strftime('%B, %Y')
    @mode = YAML.load_file("#{Rails.root}/config/application.yml")['dde_mode'] rescue 'vh'
    @gvh = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["gvh"] rescue nil
    @ta = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["ta"] rescue nil
    @outcomes = specific_outcome_sorter(NationalIdentifier.find(:all,:conditions => ['person_id IS NOT NULL']),date.end_of_month,'assigned_vh')
    render :layout => 'report'
  end

  def gvh_births_by_village
    render :layout => 'report'
  end

  def gvh_transfers_by_village
  end

  def village_reports
  end

  def village_statistics

    @village = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["village"] rescue nil
    @gvh = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["gvh"] rescue nil
    @ta = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["ta"] rescue nil
    @cumulative = cumulative_summarizer(Date.today.to_date)
    render :layout => 'report'
  end
  def report_index

    if params[:type] == "daily_summary"
      @text = "Select Date"
    else
      @text = "Select Month and Year"
    end

    render :layout => 'report'
  end

  def cohort

    @start_date,@end_date = cohort_date_range(params[:quarter])
    nat_ids = NationalIdentifier.find(:all, :conditions => ["DATE(assigned_at) >= ? AND DATE(assigned_at) <= ?", @start_date,@end_date])
    cumulative = cumulative_summarizer(@end_date)
    cohort_gender_count = gender_counter(nat_ids.map{|x| x.person})
    cohort_ages = age_categorizer(nat_ids.map{|x| x.person})
    cohort_outcomes = outcome_sorter(nat_ids.map{|x| x.person.outcome_by_date(@end_date)})
    @keys = ['Registration Details','Registered People By Age','Registered People By Outcome']
    @report = {}
    @report['Registration Details'] = [['Registered People', nat_ids.length, cumulative['count']],
                                       ['Males', cohort_gender_count['males'], cumulative['gender_count']['males']],
                                       ['Females', cohort_gender_count['females'], cumulative['gender_count']['females']]
                                      ]

    @report['Registered People By Age'] = [[ '0-12', cohort_ages['children'], cumulative['ages']['children']],
                                            ['12-21', cohort_ages['youth'], cumulative['ages']['youth']],
                                            ['22-59', cohort_ages['adults'], cumulative['ages']['adults']],
                                            ['60 and above', cohort_ages['grannies'], cumulative['ages']['grannies']]]

    @report['Registered People By Outcome'] = [ ['Alive', cohort_outcomes['Alive'], cumulative['outcomes']['Alive']],
                                                ['Died', cohort_outcomes['Dead'], cumulative['outcomes']['Dead']],
                                                ['Transferred', cohort_outcomes['Transferred'], cumulative['outcomes']['Transferred']]
                                              ]
    render :layout => 'report'
  end
  def village_outcomes_by_month
    date = params[:start_date].to_date
    @duration = date.strftime('%B, %Y')
    @mode = YAML.load_file("#{Rails.root}/config/application.yml")['dde_mode'] rescue 'vh'
    @village = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["village"] rescue nil
    @gvh = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["gvh"] rescue nil
    @ta = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["ta"] rescue nil
    nat_ids = NationalIdentifier.find(:all, :conditions => ["MONTH(assigned_at) <= ? AND YEAR(assigned_at) <= ?", date.month, date.year])
    month_nat_ids = NationalIdentifier.find(:all, :conditions => ["MONTH(assigned_at) = ? AND YEAR(assigned_at) = ?", date.month, date.year])
    @month_summary ={}
    @cumulative_summary = {}
    @month_summary['count'] = month_nat_ids.length
    @month_summary['outcomes'] = outcome_sorter(Outcome.find(:all,
                                                             :conditions => ["MONTH(outcome_date) = ? AND YEAR(outcome_date) = ?",
                                                                             date.month, date.year]).map{|x| x.name})
    @cumulative_summary['count'] = nat_ids.length
    @cumulative_summary['outcomes'] = outcome_sorter(nat_ids.map{|x| x.person.outcome_by_date(date.end_of_month)})
    @month_summary['outcomes']['Alive'] = @month_summary['count'] - (@month_summary['outcomes']['Transferred'] + @month_summary['outcomes']['Dead'])
    render :layout => 'report'
  end

  def village_births_by_month
    date = params[:start_date].to_date
    @duration = date.strftime('%B, %Y')
    @mode = YAML.load_file("#{Rails.root}/config/application.yml")['dde_mode'] rescue 'vh'
    @village = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["village"] rescue nil
    @gvh = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["gvh"] rescue nil
    @ta = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["ta"] rescue nil
    people = NationalIdentifier.find(:all, :conditions => ["DATE(assigned_at) <= ? ", date.end_of_month])
    new_borns = Person.find(:all, :conditions => ["MONTH(birthdate) = ? AND YEAR(birthdate) = ?", date.month, date.year])
    @month_summary ={}
    @cumulative_summary = {}
    @cumulative_summary['count'] = people.length
    @month_summary['count'] = new_borns.length
    @month_summary['gender_count']  = gender_counter(new_borns.map{|x| x})
    @month_summary['outcomes'] = outcome_sorter(new_borns.map{|x| x.outcome_by_date(date.end_of_month)})
    @month_summary['outcomes']['Alive'] = @month_summary['count'] - (@month_summary['outcomes']['Transferred'] + @month_summary['outcomes']['Dead'])
    @cumulative_summary['outcomes'] = outcome_sorter(people.map{|x| x.person.outcome_by_date(date.end_of_month)})
    @cumulative_summary['gender_count']  = gender_counter(people.map{|x| x.person})
    render :layout => 'report'
  end

  def village_transfers_by_month
  end

  def ta_reports
  end

  def ta_statistics
    @ta = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["ta"] rescue nil
    @cumulative = cumulative_summarizer(Date.today.to_date)
    render :layout => 'report'
  end

  def ta_outcomes_by_gvh
    date = params[:start_date].to_date
    @duration = date.strftime('%B, %Y')
    @mode = YAML.load_file("#{Rails.root}/config/application.yml")['dde_mode'] rescue 'vh'
    @gvh = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["gvh"] rescue nil
    @ta = YAML.load_file("#{Rails.root}/config/application.yml")[Rails.env]["ta"] rescue nil
    @outcomes = specific_outcome_sorter(NationalIdentifier.find(:all,:conditions => ['person_id IS NOT NULL']),date.end_of_month,'assigned_gvh')
    render :layout => 'report'
  end

  def ta_births_by_gvh
    render :layout => 'report'
  end

  def ta_transfers_by_gvh
    render :layout => 'report'
  end

  def view_village_demographics
  end

  def view_unposted_village_demographics
  end

  def village_post_demographics
  end

  def gvh_view_demographics
  end

  def gvh_view_unposted_demographics
  end

  def gvh_post_demographics
  end

  def ta_view_demographics
  end

  def ta_view_unposted_demographics
  end

  def ta_post_demographics
  end

  def cumulative_summarizer(date)
    nat_ids = NationalIdentifier.find(:all, :conditions => ["DATE(assigned_at) <= ?", date])
    cumulative_summary = {}
    cumulative_summary['count'] = nat_ids.length
    cumulative_summary['gender_count'] = gender_counter(nat_ids.map{|x| x.person})
    cumulative_summary['ages'] = age_categorizer(nat_ids.map{|x| x.person})
    cumulative_summary['outcomes'] = outcome_sorter(nat_ids.map{|x| x.person.outcome_by_date(date)})
    cumulative_summary
  end

  def gender_counter(people)
    genders = {'males' => 0, 'females'=> 0 }
    (people || []).each do |person|
       if person.gender == "Male"
          genders['males'] +=1
       else
         genders['females'] +=1
       end
    end

    return genders
  end

  def age_categorizer(people)
    age_groups = {'children' => 0, 'youth' => 0, 'adults' => 0, 'grannies' => 0}
    (people || []).each do |person|
      age = Date.today.year.to_i - person.birthdate.year.to_i
      if (age < 12)
        age_groups['children'] +=1
      elsif (age >= 12 && age <= 21)
        age_groups['youth'] +=1
      elsif (age >= 22 && age <= 59)
        age_groups['adults'] +=1
      elsif (age >= 60)
        age_groups['grannies'] +=1
      end
    end
    age_groups
  end

  def outcome_sorter(outcomes)

    outcome = {"Alive" => 0, "Transferred" => 0, "Dead" => 0}

    (outcomes || []).each do |x|
      if x == 'Dead'
        outcome['Dead'] += 1
      elsif x == 'Transfer Out'
        outcome['Transferred'] += 1
      else
        outcome['Alive'] += 1
      end
    end
    outcome
  end

  def specific_outcome_sorter(ids, end_date, mode)

    collection = {}

    (ids || []).each do |x|
      outcome = x.person.outcome_by_date(end_date)
      if collection[x[mode]].blank?
        collection[x[mode]] =  {"Alive" => 0, "Transferred" => 0, "Dead" => 0, "Total" => 0}
      end
      if outcome == 'Dead'
        collection[x[mode]]['Dead'] += 1
      elsif outcome == 'Transfer Out'
        collection[x[mode]]['Transferred'] += 1
      else
        collection[x[mode]]['Alive'] += 1
      end
      collection[x[mode]]["Total"] += 1
    end
    collection
  end

  def cohort_date_range(quarter)


    quarter, quarter_year = quarter.humanize.split(" ")

    quarter_start_dates = ["#{quarter_year}-01-01".to_date, "#{quarter_year}-04-01".to_date, "#{quarter_year}-07-01".to_date, "#{quarter_year}-10-01".to_date]
    quarter_end_dates   = ["#{quarter_year}-03-31".to_date, "#{quarter_year}-06-30".to_date, "#{quarter_year}-09-30".to_date, "#{quarter_year}-12-31".to_date]

    current_quarter   = (quarter.match(/\d+/).to_s.to_i - 1)
    quarter_beginning = quarter_start_dates[current_quarter]
    quarter_ending    = quarter_end_dates[current_quarter]

    date_range = [quarter_beginning, quarter_ending]
    return date_range
  end

end

